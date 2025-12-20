#!/usr/bin/env python3
"""
API Test Suite для доски объявлений
Читает конфигурацию из .env файла или использует значения по умолчанию
"""

import os
import sys
import json
import requests
from typing import Dict, Any, Optional
from datetime import datetime
from dotenv import load_dotenv

class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    RESET = '\033[0m'
    BOLD = '\033[1m'

class APITester:
    def __init__(self):
        load_dotenv()
        
        self.base_url = os.getenv('API_BASE_URL', 'http://localhost:8082')
        self.test_username = os.getenv('TEST_USERNAME', 'user1')
        self.test_password = os.getenv('TEST_PASSWORD', 'password')
        self.test_username2 = os.getenv('TEST_USERNAME2', 'user2')
        self.test_password2 = os.getenv('TEST_PASSWORD2', 'password')
        self.admin_username = os.getenv('ADMIN_USERNAME', 'admin1')
        self.admin_password = os.getenv('ADMIN_PASSWORD', 'password')
        
        self.session = requests.Session()
        self.session.verify = False
        self.access_token = None
        
        self.stats = {
            'total': 0,
            'passed': 0,
            'failed': 0,
            'skipped': 0
        }
        
        self.created_resources = {
            'categories': [],
            'listings': [],
            'messages': [],
            'reports': [],
            'users': []
        }
    
    def print_header(self, text: str):
        print(f"\n{Colors.BOLD}{Colors.CYAN}{'='*60}{Colors.RESET}")
        print(f"{Colors.BOLD}{Colors.CYAN}{text}{Colors.RESET}")
        print(f"{Colors.BOLD}{Colors.CYAN}{'='*60}{Colors.RESET}\n")
    
    def print_test(self, name: str, passed: bool, details: str = "", show_error_body: bool = False, error_data: Optional[Dict] = None):
        self.stats['total'] += 1
        if passed:
            self.stats['passed'] += 1
            status = f"{Colors.GREEN}✓ PASSED{Colors.RESET}"
        else:
            self.stats['failed'] += 1
            status = f"{Colors.RED}✗ FAILED{Colors.RESET}"
        
        print(f"{status} {name}")
        if details:
            print(f"    {details}")
        if not passed and show_error_body and error_data:
            if isinstance(error_data, dict):
                error_field = error_data.get('error', '')
                message_field = error_data.get('message', '')
                
                if error_field and message_field:
                    print(f"    {Colors.YELLOW}Детали ошибки: {error_field} - {message_field}{Colors.RESET}")
                elif error_field:
                    print(f"    {Colors.YELLOW}Детали ошибки: {error_field}{Colors.RESET}")
                elif message_field:
                    print(f"    {Colors.YELLOW}Детали ошибки: {message_field}{Colors.RESET}")
                elif error_data:
                    print(f"    {Colors.YELLOW}Детали ошибки: {json.dumps(error_data, ensure_ascii=False, indent=2)}{Colors.RESET}")
            else:
                error_msg = str(error_data)
                if error_msg and error_msg != '{}' and error_msg != str({}):
                    print(f"    {Colors.YELLOW}Детали ошибки: {error_msg}{Colors.RESET}")
    
    def print_skipped(self, name: str, reason: str = ""):
        self.stats['total'] += 1
        self.stats['skipped'] += 1
        print(f"{Colors.YELLOW}⊘ SKIPPED{Colors.RESET} {name}")
        if reason:
            print(f"    {reason}")
    
    def make_request(self, method: str, endpoint: str, data: Optional[Dict] = None, 
                    expected_status: Optional[int] = None, use_auth: bool = True) -> Dict[str, Any]:
        url = f"{self.base_url}{endpoint}"
        headers = {}
        
        if use_auth and self.access_token:
            headers['Authorization'] = f'Bearer {self.access_token}'
        
        try:
            if method.upper() == 'GET':
                response = self.session.get(url, headers=headers)
            elif method.upper() == 'POST':
                if data and 'password' in data:
                    data_copy = data.copy()
                    data_copy['password'] = '***' if data_copy.get('password') else None
                response = self.session.post(url, json=data, headers=headers)
            elif method.upper() == 'PUT':
                response = self.session.put(url, json=data, headers=headers)
            elif method.upper() == 'DELETE':
                response = self.session.delete(url, headers=headers)
            else:
                return {'ok': False, 'status': 0, 'error': f'Unknown method: {method}'}
            
            try:
                response_data = response.json() if response.content else {}
            except:
                response_data = {'text': response.text}
            
            result = {
                'ok': response.ok,
                'status': response.status_code,
                'data': response_data
            }
            
            if expected_status and response.status_code != expected_status:
                result['ok'] = False
            
            return result
        except requests.exceptions.ConnectionError:
            return {'ok': False, 'status': 0, 'error': 'Connection refused - сервер не запущен?'}
        except Exception as e:
            return {'ok': False, 'status': 0, 'error': str(e)}
    
    def login(self, username: str, password: str) -> bool:
        try:
            response = self.session.post(
                f"{self.base_url}/auth/login",
                json={'username': username, 'password': password},
                headers={'Content-Type': 'application/json'}
            )
            if response.ok:
                response_data = response.json()
                self.access_token = response_data.get('accessToken')
                if self.access_token:
                    return True
                else:
                    print(f"    {Colors.RED}Ошибка входа: токен не получен{Colors.RESET}")
                    return False
            return False
        except Exception as e:
            print(f"    {Colors.RED}Ошибка входа: {e}{Colors.RESET}")
            self.access_token = None
            return False
    
    def check_admin(self) -> bool:
        me_result = self.make_request('GET', '/api/users/me')
        if me_result['ok']:
            user_data = me_result.get('data', {})
            user_role = user_data.get('role', '')
            username = user_data.get('username', '')
            is_admin = user_role == 'ADMIN' or user_role == 'ROLE_ADMIN'
            if not is_admin:
                print(f"    {Colors.YELLOW}Проверка прав: пользователь {username} имеет роль '{user_role}', ожидалась 'ADMIN'{Colors.RESET}")
            return is_admin
        return False
    
    def test_connectivity(self) -> bool:
        result = self.make_request('GET', '/api/categories')
        if result['status'] == 0:
            self.print_test('Проверка подключения к серверу', False, 
                          f"Не удалось подключиться к {self.base_url}\n"
                          f"    Убедитесь, что сервер запущен")
            return False
        self.print_test('Проверка подключения к серверу', True, 
                       f"Подключение к {self.base_url} успешно")
        return True
    
    def test_auth(self):
        self.print_header("🔐 ТЕСТЫ АУТЕНТИФИКАЦИИ")
        
        old_token = self.access_token
        self.access_token = None
        
        result = self.make_request('GET', '/api/users/me', use_auth=False)
        if result['status'] == 0:
            self.print_test('GET /api/users/me (без аутентификации)', False,
                          f"Сетевая ошибка: {result.get('error', 'неизвестная ошибка')}")
            self.access_token = old_token
            return
        
        if not result['ok'] and result['status'] in [401, 403]:
            self.print_test('GET /api/users/me (без аутентификации)', True,
                          f"Правильно отклонен со статусом {result['status']}")
        elif result['ok']:
            self.print_test('GET /api/users/me (без аутентификации)', False,
                          f"ОШИБКА БЕЗОПАСНОСТИ: запрос прошел без аутентификации!")
        else:
            self.print_test('GET /api/users/me (без аутентификации)', False,
                          f"Неожиданный статус: {result['status']} (ожидался 401/403)")
        
        self.access_token = old_token
        
        login_result = self.login(self.test_username, self.test_password)
        self.print_test('POST /auth/login', login_result,
                       f"Вход пользователя: {self.test_username}")
        
        if login_result:
            result = self.make_request('GET', '/api/users/me')
            self.print_test('GET /api/users/me (после входа)', result['ok'],
                          f"Статус: {result['status']}")
        
        register_data = {
            'username': f'testuser_{int(datetime.now().timestamp())}',
            'email': f'test_{int(datetime.now().timestamp())}@example.com',
            'password': 'TestPassword123!!',
            'role': 'USER'
        }
        result = self.make_request('POST', '/auth/register', register_data, expected_status=201)
        is_success = result['ok'] and result['status'] == 201
        if not is_success:
            error_info = result.get('data', {})
            if isinstance(error_info, dict):
                error_msg = error_info.get('error') or error_info.get('message', '')
                if 'already exists' in str(error_info).lower() or 'уже существует' in str(error_info).lower():
                    self.print_test('POST /auth/register', False,
                                  f"Статус: {result['status']} (пользователь уже существует)",
                                  show_error_body=True,
                                  error_data=error_info)
                elif 'password' in str(error_info).lower() or 'пароль' in str(error_info).lower():
                    full_error_text = str(error_info)
                    self.print_test('POST /auth/register', False,
                                  f"Статус: {result['status']} (ошибка валидации пароля)",
                                  show_error_body=True,
                                  error_data=error_info)
                else:
                    self.print_test('POST /auth/register', False,
                                  f"Статус: {result['status']}",
                                  show_error_body=True,
                                  error_data=error_info)
            else:
                self.print_test('POST /auth/register', False,
                              f"Статус: {result['status']}",
                              show_error_body=True,
                              error_data={'data': error_info})
        else:
            self.print_test('POST /auth/register', True,
                          f"Статус: {result['status']}")
        if result['ok'] and 'id' in result.get('data', {}):
            self.created_resources['users'].append(result['data']['id'])
    
    def test_categories(self):
        self.print_header("📁 ТЕСТЫ КАТЕГОРИЙ")
        
        if not self.login(self.test_username, self.test_password):
            self.print_skipped('Все тесты категорий', 'Не удалось войти в систему')
            return
        
        result = self.make_request('GET', '/api/categories')
        count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
        self.print_test('GET /api/categories', result['ok'],
                      f"Статус: {result['status']}, найдено категорий: {count}")
        
        if result['ok'] and isinstance(result.get('data'), list) and len(result['data']) > 0:
            first_category = result['data'][0]
            category_id = first_category.get('id')
            if category_id:
                result = self.make_request('GET', f'/api/categories/{category_id}')
                self.print_test(f'GET /api/categories/{category_id}', result['ok'],
                              f"Статус: {result['status']}")
        
        create_data = {
            'name': f'Test Category {int(datetime.now().timestamp())}',
            'description': 'Test category description'
        }
        result = self.make_request('POST', '/api/categories', create_data, expected_status=201)
        self.print_test('POST /api/categories', result['ok'] and result['status'] == 201,
                       f"Статус: {result['status']}")
        
        if result['ok'] and 'id' in result.get('data', {}):
            category_id = result['data']['id']
            self.created_resources['categories'].append(category_id)
            
            update_data = {
                'name': f'Updated Category {int(datetime.now().timestamp())}',
                'description': 'Updated description'
            }
            result = self.make_request('PUT', f'/api/categories/{category_id}', update_data)
            self.print_test(f'PUT /api/categories/{category_id}', result['ok'],
                          f"Статус: {result['status']}")
            
            result = self.make_request('DELETE', f'/api/categories/{category_id}', expected_status=204)
            if result['status'] == 403:
                self.print_test(f'DELETE /api/categories/{category_id}', False,
                              f"Статус: {result['status']} (требуются права ADMIN - это ожидаемо для обычного пользователя)",
                              show_error_body=False,
                              error_data=None)
            elif result['ok'] and result['status'] == 204:
                self.print_test(f'DELETE /api/categories/{category_id}', True,
                              f"Статус: {result['status']}")
                self.created_resources['categories'].remove(category_id)
            else:
                self.print_test(f'DELETE /api/categories/{category_id}', False,
                              f"Статус: {result['status']}",
                              show_error_body=True,
                              error_data=result.get('data', {}))
    
    def test_listings(self):
        self.print_header("📋 ТЕСТЫ ОБЪЯВЛЕНИЙ")
        
        if not self.login(self.test_username, self.test_password):
            self.print_skipped('Все тесты объявлений', 'Не удалось войти в систему')
            return
        
        result = self.make_request('GET', '/api/listings')
        count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
        self.print_test('GET /api/listings', result['ok'],
                      f"Статус: {result['status']}, найдено объявлений: {count}")
        
        result = self.make_request('GET', '/api/listings/all')
        count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
        self.print_test('GET /api/listings/all', result['ok'],
                      f"Статус: {result['status']}, найдено объявлений: {count}")
        
        categories_result = self.make_request('GET', '/api/categories')
        category_id = 1
        if categories_result['ok'] and isinstance(categories_result.get('data'), list) and len(categories_result['data']) > 0:
            category_id = categories_result['data'][0].get('id', 1)
        
        create_data = {
            'title': f'Test Listing {int(datetime.now().timestamp())}',
            'description': 'Test listing description',
            'price': 10000.50,
            'categoryId': category_id
        }
        result = self.make_request('POST', '/api/listings', create_data, expected_status=201)
        self.print_test('POST /api/listings', result['ok'] and result['status'] == 201,
                       f"Статус: {result['status']}")
        
        if result['ok'] and 'id' in result.get('data', {}):
            listing_id = result['data']['id']
            self.created_resources['listings'].append(listing_id)
            
            result = self.make_request('GET', f'/api/listings/{listing_id}')
            self.print_test(f'GET /api/listings/{listing_id}', result['ok'],
                          f"Статус: {result['status']}")
            
            result = self.make_request('GET', f'/api/listings/category/{category_id}')
            count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
            self.print_test(f'GET /api/listings/category/{category_id}', result['ok'],
                          f"Статус: {result['status']}, найдено объявлений: {count}")
            
            me_result = self.make_request('GET', '/api/users/me')
            if me_result['ok'] and 'id' in me_result.get('data', {}):
                user_id = me_result['data']['id']
                result = self.make_request('GET', f'/api/listings/user/{user_id}')
                count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
                self.print_test(f'GET /api/listings/user/{user_id}', result['ok'],
                              f"Статус: {result['status']}, найдено объявлений: {count}")
                
                result = self.make_request('GET', '/api/listings/user/me')
                count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
                self.print_test('GET /api/listings/user/me', result['ok'],
                              f"Статус: {result['status']}, найдено объявлений: {count}")
            
            update_data = {
                'title': f'Updated Listing {int(datetime.now().timestamp())}',
                'description': 'Updated description',
                'price': 15000.75,
                'categoryId': category_id
            }
            result = self.make_request('PUT', f'/api/listings/{listing_id}', update_data)
            self.print_test(f'PUT /api/listings/{listing_id}', result['ok'],
                          f"Статус: {result['status']}")
            
            result = self.make_request('POST', f'/api/listings/{listing_id}/deactivate')
            self.print_test(f'POST /api/listings/{listing_id}/deactivate', result['ok'],
                          f"Статус: {result['status']}")
            
            result = self.make_request('DELETE', f'/api/listings/{listing_id}', expected_status=204)
            self.print_test(f'DELETE /api/listings/{listing_id}', 
                          result['ok'] and result['status'] == 204,
                          f"Статус: {result['status']}")
            if result['ok']:
                self.created_resources['listings'].remove(listing_id)
    
    def test_messages(self):
        self.print_header("💬 ТЕСТЫ СООБЩЕНИЙ")
        
        if not self.login(self.test_username, self.test_password):
            self.print_skipped('Все тесты сообщений', 'Не удалось войти в систему')
            return
        
        me_result = self.make_request('GET', '/api/users/me')
        if not me_result['ok'] or 'id' not in me_result.get('data', {}):
            self.print_skipped('Все тесты сообщений', 'Не удалось получить текущего пользователя')
            return
        
        current_user_id = me_result['data']['id']
        
        categories_result = self.make_request('GET', '/api/categories')
        category_id = 1
        if categories_result['ok'] and isinstance(categories_result.get('data'), list) and len(categories_result['data']) > 0:
            category_id = categories_result['data'][0].get('id', 1)
        
        create_listing_data = {
            'title': f'Test Listing for Messages {int(datetime.now().timestamp())}',
            'description': 'Test listing for message testing',
            'price': 5000.00,
            'categoryId': category_id
        }
        listing_result = self.make_request('POST', '/api/listings', create_listing_data, expected_status=201)
        
        if not listing_result['ok'] or 'id' not in listing_result.get('data', {}):
            self.print_skipped('Все тесты сообщений', 'Не удалось создать объявление')
            return
        
        listing_id = listing_result['data']['id']
        self.created_resources['listings'].append(listing_id)
        listing_owner_id = listing_result['data'].get('user', {}).get('id', current_user_id)
        
        if not self.login(self.test_username2, self.test_password2):
            self.print_skipped('Тесты создания сообщений', 'Не удалось войти вторым пользователем')
            self.login(self.test_username, self.test_password)
            return
        
        user2_result = self.make_request('GET', '/api/users/me')
        user2_id = user2_result.get('data', {}).get('id') if user2_result['ok'] else None
        
        if user2_id and user2_id != listing_owner_id:
            create_message_data = {
                'text': f'Test message {int(datetime.now().timestamp())}',
                'listingId': listing_id,
                'receiverId': listing_owner_id
            }
            result = self.make_request('POST', '/api/messages', create_message_data, expected_status=201)
            self.print_test('POST /api/messages', result['ok'] and result['status'] == 201,
                          f"Статус: {result['status']}")
            
            if result['ok'] and 'id' in result.get('data', {}):
                message_id = result['data']['id']
                self.created_resources['messages'].append(message_id)
                
                result = self.make_request('GET', f'/api/messages/{message_id}')
                self.print_test(f'GET /api/messages/{message_id}', result['ok'],
                              f"Статус: {result['status']}")
                
                result = self.make_request('GET', f'/api/messages/listing/{listing_id}')
                count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
                self.print_test(f'GET /api/messages/listing/{listing_id}', result['ok'],
                              f"Статус: {result['status']}, найдено сообщений: {count}")
                
                result = self.make_request('GET', f'/api/messages/listing/{listing_id}/conversation')
                count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
                self.print_test(f'GET /api/messages/listing/{listing_id}/conversation', result['ok'],
                              f"Статус: {result['status']}, найдено сообщений: {count}")
                
                result = self.make_request('GET', '/api/messages/sent')
                count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
                self.print_test('GET /api/messages/sent', result['ok'],
                              f"Статус: {result['status']}, найдено сообщений: {count}")
                
                result = self.make_request('GET', '/api/messages/received')
                count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
                self.print_test('GET /api/messages/received', result['ok'],
                              f"Статус: {result['status']}, найдено сообщений: {count}")
                
                update_data = {'text': f'Updated message {int(datetime.now().timestamp())}'}
                result = self.make_request('PUT', f'/api/messages/{message_id}', update_data)
                self.print_test(f'PUT /api/messages/{message_id}', result['ok'],
                              f"Статус: {result['status']}")
                
                result = self.make_request('POST', f'/api/messages/{message_id}/read')
                if not result['ok'] and result['status'] == 403:
                    self.print_test(f'POST /api/messages/{message_id}/read', False,
                                  f"Статус: {result['status']} (можно пометить только полученные сообщения - это ожидаемо)",
                                  show_error_body=True,
                                  error_data=result.get('data', {}))
                else:
                    self.print_test(f'POST /api/messages/{message_id}/read', result['ok'],
                                  f"Статус: {result['status']}")
                
                result = self.make_request('DELETE', f'/api/messages/{message_id}', expected_status=204)
                self.print_test(f'DELETE /api/messages/{message_id}', 
                              result['ok'] and result['status'] == 204,
                              f"Статус: {result['status']}")
                if result['ok']:
                    self.created_resources['messages'].remove(message_id)
        
        self.login(self.test_username, self.test_password)
        result = self.make_request('DELETE', f'/api/listings/{listing_id}')
        if result['ok']:
            self.created_resources['listings'].remove(listing_id)
    
    def test_reports(self):
        self.print_header("⚠️ ТЕСТЫ ЖАЛОБ")
        
        if not self.login(self.test_username, self.test_password):
            self.print_skipped('Все тесты жалоб', 'Не удалось войти в систему')
            return
        
        result = self.make_request('GET', '/api/reports')
        count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
        self.print_test('GET /api/reports', result['ok'],
                      f"Статус: {result['status']}, найдено жалоб: {count}")
        
        categories_result = self.make_request('GET', '/api/categories')
        category_id = 1
        if categories_result['ok'] and isinstance(categories_result.get('data'), list) and len(categories_result['data']) > 0:
            category_id = categories_result['data'][0].get('id', 1)
        
        create_listing_data = {
            'title': f'Test Listing for Reports {int(datetime.now().timestamp())}',
            'description': 'Test listing for report testing',
            'price': 3000.00,
            'categoryId': category_id
        }
        listing_result = self.make_request('POST', '/api/listings', create_listing_data, expected_status=201)
        
        if not listing_result['ok'] or 'id' not in listing_result.get('data', {}):
            self.print_skipped('Все тесты жалоб', 'Не удалось создать объявление')
            return
        
        listing_id = listing_result['data']['id']
        self.created_resources['listings'].append(listing_id)
        
        if not self.login(self.test_username2, self.test_password2):
            self.print_skipped('Тесты создания жалоб', 'Не удалось войти вторым пользователем')
            self.login(self.test_username, self.test_password)
            return
        
        create_report_data = {
            'reason': f'Test report reason {int(datetime.now().timestamp())}',
            'listingId': listing_id
        }
        result = self.make_request('POST', '/api/reports', create_report_data, expected_status=201)
        self.print_test('POST /api/reports', result['ok'] and result['status'] == 201,
                      f"Статус: {result['status']}")
        
        if result['ok'] and 'id' in result.get('data', {}):
            report_id = result['data']['id']
            self.created_resources['reports'].append(report_id)
            
            result = self.make_request('GET', f'/api/reports/{report_id}')
            self.print_test(f'GET /api/reports/{report_id}', result['ok'],
                          f"Статус: {result['status']}")
            
            result = self.make_request('GET', f'/api/reports/listing/{listing_id}')
            count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
            self.print_test(f'GET /api/reports/listing/{listing_id}', result['ok'],
                          f"Статус: {result['status']}, найдено жалоб: {count}")
            
            result = self.make_request('GET', '/api/reports/user')
            count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
            self.print_test('GET /api/reports/user', result['ok'],
                          f"Статус: {result['status']}, найдено жалоб: {count}")
            
            result = self.make_request('GET', '/api/reports/status/PENDING')
            count = len(result.get('data', [])) if isinstance(result.get('data'), list) else 0
            self.print_test('GET /api/reports/status/PENDING', result['ok'],
                          f"Статус: {result['status']}, найдено жалоб: {count}")
            
            if self.login(self.admin_username, self.admin_password):
                if not self.check_admin():
                    me_result = self.make_request('GET', '/api/users/me')
                    user_data = me_result.get('data', {}) if me_result['ok'] else {}
                    user_role = user_data.get('role', 'неизвестна')
                    username = user_data.get('username', self.admin_username)
                    self.print_skipped(f'PUT/DELETE /api/reports/{report_id}', 
                                     f'Пользователь {username} имеет роль "{user_role}", требуется "ADMIN". Проверьте, что пользователь {self.admin_username} существует и имеет роль ADMIN в базе данных.')
                    self.login(self.test_username, self.test_password)
                    return
                
                update_status_data = {'status': 'APPROVED'}
                result = self.make_request('PUT', f'/api/reports/{report_id}/status', update_status_data)
                if not result['ok']:
                    error_info = result.get('data', {})
                    error_msg = error_info.get('error') or error_info.get('message', '') if isinstance(error_info, dict) else str(error_info)
                    self.print_test(f'PUT /api/reports/{report_id}/status', False,
                                  f"Статус: {result['status']}",
                                  show_error_body=True,
                                  error_data=error_info)
                else:
                    self.print_test(f'PUT /api/reports/{report_id}/status', True,
                                  f"Статус: {result['status']}")
                
                if not self.check_admin():
                    self.print_test(f'DELETE /api/reports/{report_id}', False,
                                  f"Проверка прав: пользователь не является ADMIN",
                                  show_error_body=False,
                                  error_data=None)
                else:
                    result = self.make_request('DELETE', f'/api/reports/{report_id}', expected_status=204)
                    if result['ok'] and result['status'] == 204:
                        self.print_test(f'DELETE /api/reports/{report_id}', True,
                                      f"Статус: {result['status']}")
                        self.created_resources['reports'].remove(report_id)
                    elif result['status'] == 403:
                        self.print_test(f'DELETE /api/reports/{report_id}', False,
                                      f"Статус: {result['status']} (требуются права ADMIN, но пользователь вошел как админ - возможно проблема с сессией)",
                                      show_error_body=True,
                                      error_data=result.get('data', {}))
                    else:
                        self.print_test(f'DELETE /api/reports/{report_id}', False,
                                      f"Статус: {result['status']}",
                                      show_error_body=True,
                                      error_data=result.get('data', {}))
            else:
                self.print_skipped(f'PUT/DELETE /api/reports/{report_id}', 
                                 f'Не удалось войти как {self.admin_username} с паролем {self.admin_password}')
        
        self.login(self.test_username, self.test_password)
        result = self.make_request('DELETE', f'/api/listings/{listing_id}')
        if result['ok']:
            self.created_resources['listings'].remove(listing_id)
    
    def test_users(self):
        self.print_header("👤 ТЕСТЫ ПОЛЬЗОВАТЕЛЕЙ")
        
        if not self.login(self.test_username, self.test_password):
            self.print_skipped('Все тесты пользователей', 'Не удалось войти в систему')
            return
        
        result = self.make_request('GET', '/api/users/me')
        self.print_test('GET /api/users/me', result['ok'],
                      f"Статус: {result['status']}")
        
        if result['ok'] and 'id' in result.get('data', {}):
            user_id = result['data']['id']
            result = self.make_request('GET', f'/api/users/{user_id}')
            self.print_test(f'GET /api/users/{user_id}', result['ok'],
                          f"Статус: {result['status']}")
        
        create_user_data = {
            'username': f'testuser_{int(datetime.now().timestamp())}',
            'email': f'test_{int(datetime.now().timestamp())}@example.com',
            'password': 'TestPassword123!!',
            'role': 'USER'
        }
        result = self.make_request('POST', '/api/users/create', create_user_data)
        if not result['ok']:
            error_info = result.get('data', {})
            error_msg = ''
            if isinstance(error_info, dict):
                error_msg = error_info.get('error') or error_info.get('message', '')
            else:
                error_msg = str(error_info)
            
            full_error = str(error_info).lower()
            if 'already exists' in full_error or 'уже существует' in full_error:
                self.print_test('POST /api/users/create', False,
                              f"Статус: {result['status']} (пользователь уже существует)",
                              show_error_body=True,
                              error_data=error_info)
            elif 'password' in full_error or 'пароль' in full_error:
                self.print_test('POST /api/users/create', False,
                              f"Статус: {result['status']} (ошибка валидации пароля: пароль должен содержать минимум 8 символов, цифру и спецсимвол)",
                              show_error_body=True,
                              error_data=error_info)
            else:
                self.print_test('POST /api/users/create', False,
                              f"Статус: {result['status']}",
                              show_error_body=True,
                              error_data=error_info)
        else:
            self.print_test('POST /api/users/create', True,
                          f"Статус: {result['status']}")
        if result['ok'] and 'id' in result.get('data', {}):
            self.created_resources['users'].append(result['data']['id'])
    
    def print_summary(self):
        self.print_header("📊 СВОДКА РЕЗУЛЬТАТОВ")
        
        print(f"{Colors.BOLD}Всего тестов:{Colors.RESET} {self.stats['total']}")
        print(f"{Colors.GREEN}Успешно:{Colors.RESET} {self.stats['passed']}")
        print(f"{Colors.RED}Ошибок:{Colors.RESET} {self.stats['failed']}")
        print(f"{Colors.YELLOW}Пропущено:{Colors.RESET} {self.stats['skipped']}")
        
        if self.stats['total'] > 0:
            success_rate = (self.stats['passed'] / self.stats['total']) * 100
            print(f"\n{Colors.BOLD}Успешность:{Colors.RESET} {success_rate:.1f}%")
        
        if self.stats['failed'] > 0:
            print(f"\n{Colors.RED}⚠️ Обнаружены ошибки! Проверьте вывод выше.{Colors.RESET}")
        elif self.stats['total'] > 0:
            print(f"\n{Colors.GREEN}✓ Все тесты прошли успешно!{Colors.RESET}")
    
    def run_all_tests(self):
        print(f"\n{Colors.BOLD}{Colors.BLUE}")
        print("╔" + "═" * 58 + "╗")
        print("║" + " " * 10 + "API TEST SUITE - ДОСКА ОБЪЯВЛЕНИЙ" + " " * 10 + "║")
        print("╚" + "═" * 58 + "╝")
        print(f"{Colors.RESET}")
        print(f"Base URL: {Colors.CYAN}{self.base_url}{Colors.RESET}")
        print(f"Test User: {Colors.CYAN}{self.test_username}{Colors.RESET}")
        print(f"Test User 2: {Colors.CYAN}{self.test_username2}{Colors.RESET}")
        print(f"Admin User: {Colors.CYAN}{self.admin_username}{Colors.RESET}")
        
        if not self.test_connectivity():
            print(f"\n{Colors.RED}Не удалось подключиться к серверу. Завершение тестов.{Colors.RESET}")
            return
        
        self.test_auth()
        self.test_categories()
        self.test_listings()
        self.test_messages()
        self.test_reports()
        self.test_users()
        
        self.print_summary()

if __name__ == '__main__':
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    
    tester = APITester()
    tester.run_all_tests()

