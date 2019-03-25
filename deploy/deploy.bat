@echo off
title deployment-script
setlocal enabledelayedexpansion

REM color codes
set blu=[94m
set grn=[92m
set red=[91m
set bld=[1m
set clr=[0m

REM command prefixes
set error_prefix_=%red%%bld%[Deployment Script]%clr%%clr%
set script_prefix_=%grn%%bld%[Deployment Script]%clr%%clr%

REM parameters
set binary_build=scripts\binary-build.bat
set docker_build=scripts\docker-build.bat
set docker_push=scripts\docker-push.bat
set registry_name=riskreg

REM arguments
set arguments_=%*
set login=false
for /f "tokens=1*" %%a in ('echo %arguments_%') do (
  if "%%a"=="--login" (
    set login=true
  )
)

call :start_message

set commands_found=true
call :command_check "npm"
call :command_check "sbt"
call :command_check "docker"
call :command_check "powershell"
call :command_check "javac"

if "!commands_found!"=="false" (
  call :exit_message
  exit /b %errorlevel%
)

call :inter_message
call %binary_build%
call :inter_message
call %docker_build%
call :inter_message
if "%login%"=="true" (
  set commands_found=true
  call :command_check "az"
  if "!commands_found!"=="false" (
    call :exit_message
    exit /b %errorlevel%
  )
  echo %script_prefix_% Preparing to log into container registry
  call az login
  call az acr login --name %registry_name%
  call :inter_message
)
call %docker_push%
call :inter_message
echo %script_prefix_% Finished

call :exit_message

exit /b2

REM ---------------------------------------

:command_check
  set command_found=true
  WHERE >nul 2>nul %1
  if %ERRORLEVEL% NEQ 0 set command_found=false
  if "%command_found%"=="false" (
    echo %error_prefix_% Required command %1 was not found on the path.
    set commands_found=false
  ) else (
    echo %script_prefix_% Check for %1 passed
  )
  exit /b

:inter_message
  echo %blu%==================================================================================================%clr%
  exit /b

:start_message
  echo %blu%---------------------------------------%clr%%bld%^< Deployment Script^>%clr%%blu%---------------------------------------%clr%
  exit /b

:exit_message
  echo %blu%---------------------------------------%clr%%bld%^</Deployment Script^>%clr%%blu%---------------------------------------%clr%
  exit /b

REM ---------------------------------------