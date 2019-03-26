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
set "lines_========================================"

REM scripts
set binary_build=scripts\binary-build.bat
set docker_build=scripts\docker-build.bat
set docker_push=scripts\docker-push.bat

REM parameters
set registry_name=riskreg
set registry_server=%registry_name%.azurecr.io
set local_image=risk-main
set remote_image_name=risk-main
set remote_image_tag=latest
set resource_group=cs2340-risk
set cluster_name=cs2340-risk
set deployment_name=risk-main

REM arguments
set login=false
set nopush=false
set deploy=false
set nobuild=false
set init=false
for %%a in (%*) do (
  if "%%a"=="--login" (
    set login=true
  )
  if "%%a"=="--deploy" (
    set deploy=true
  )
  if "%%a"=="--nobuild" (
    set nobuild=true
  )
  if "%%a"=="--nopush" (
    set nopush=true
  )
  if "%%a"=="--init" (
    set init=true
  )
)

call :start_message

set commands_found=true

if "%nobuild%"=="false" (
  call :command_check "npm"
  call :command_check "sbt"
  call :command_check "powershell"
  call :command_check "docker"
  call :command_check "javac"
) else (
  if "%nopush%"=="false" (
    call :command_check "docker"
  )
)
if "%login%"=="true" (
  call :command_check "az"
)
if "%deploy%"=="true" (
  call :command_check "kubectl"
)
call :inter_message

if "!commands_found!"=="false" (
  call :exit_message
  exit /b %errorlevel%
)

if "%nobuild%"=="false" (
  call %binary_build%
  call :inter_message
  call %docker_build% %local_image%
  call :inter_message
)
if "%login%"=="true" (
  echo %script_prefix_% Preparing to log into container registry
  call az login
  call az acr login --name %registry_name%
  echo %script_prefix_% Preparing to attach to Kubernetes cluster
  call az aks get-credentials --resource-group %resource_group% --name %cluster_name%
  call :inter_message
)
if "%nopush%"=="false" (
  call %docker_push% %local_image% %registry_server% %remote_image_name%:%remote_image_tag%
  call :inter_message
)
if "%deploy%"=="true" (
  echo %script_prefix_% Preparing to update image on deployment/%deployment_name%
  set image_command=call kubectl set image deployments/%deployment_name% %remote_image_name%=%registry_server%/%remote_image_name%
  set image_command_tag=!image_command!:%remote_image_tag%
  call :capture_output "!image_command!"
  if "!out!"=="" (
    echo %script_prefix_% Image update ineffective, trying deploy with tag
    call :capture_output "!image_command_tag!"
    if "!out!"=="" (
      echo %error_prefix_% Image update failed
    ) else (
      call :update_success
    )
  ) else (
    call :update_success
  )
  call :inter_message
)
echo %script_prefix_% Finished

call :exit_message
exit /b2

REM ---------------------------------------

:update_success
  echo !out!
  echo %script_prefix_% Image updated successfully
  exit /b

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

:capture_output
  set lf=-
  for /F "delims=" %%i in ('%1') do (
    if "!out!"=="" (
      set out=%%i
    ) else (
      set out=!out!%lf%%%i
    )
  )
  exit /b

:inter_message
  echo %blu%--------------------------------------------------------------------------------------------------%clr%
  exit /b

:start_message
  echo %blu%%lines_%%clr%%bld%^< Deployment Script^>%clr%%blu%%lines_%%clr%
  exit /b

:exit_message
  echo %blu%%lines_%%clr%%bld%^</Deployment Script^>%clr%%blu%%lines_%%clr%
  exit /b

REM ---------------------------------------
