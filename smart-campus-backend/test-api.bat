@echo off
chcp 65001 >nul
echo ========================================
echo 智慧校园后端 API 测试脚本
echo ========================================
echo.

echo [1/8] 测试健康检查...
curl -s http://localhost:8080/actuator/health 2>nul || echo   → 启动中，请稍候...
echo.

echo [2/8] 测试用户注册...
curl -X POST http://localhost:8080/api/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"test001\",\"password\":\"123456\"}"
echo.
echo.

echo [3/8] 测试用户登录...
curl -X POST http://localhost:8080/api/auth/login?username=test001&password=123456
echo.
echo.

echo [4/8] 测试检查用户名是否存在...
curl http://localhost:8080/api/auth/check?username=test001
echo.
echo.

echo [5/8] 测试获取所有POI...
curl http://localhost:8080/api/pois
echo.
echo.

echo [6/8] 测试创建POI（需要登录）...
curl -X POST http://localhost:8080/api/auth/login?username=test001&password=123456 -s | findstr "\"token\"" > temp_token.txt
for /f "tokens=2" %%a in ('findstr "\"token\"" temp_token.txt') do set TOKEN_LINE=%%a
set TOKEN=%TOKEN_LINE:"=%
set TOKEN=%TOKEN:,=%
set TOKEN=%TOKEN:}=%
curl -X POST http://localhost:8080/api/pois ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -d "{\"name\":\"测试图书馆\",\"category\":\"学习\",\"description\":\"校园图书馆\",\"latitude\":39.9042,\"longitude\":116.4074}"
del temp_token.txt
echo.
echo.

echo [7/8] 测试按名称搜索POI...
curl http://localhost:8080/api/pois/search?name=图书馆
echo.
echo.

echo [8/8] 测试获取所有分类...
curl http://localhost:8080/api/pois/categories
echo.
echo.

echo ========================================
echo 测试完成！
echo ========================================
pause
