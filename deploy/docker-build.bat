@echo off
title production-script
setlocal enabledelayedexpansion
PUSHD ..

REM command prefixes
set script_prefix=[92m[Docker Script][0m
set intermediate_prefix=[93m[Docker Script] -[0m
set error_prefix=[91m[Docker Script][0m
set finished_prefix=[94m[Docker Script][0m

REM parameters
set deploy_root=deploy
set dist_origin=target\universal
set dist_target=deploy\svc
set start_folder=bin
set dockerfile=deploy\Dockerfile
set image_name=risk-main
set start_script=bootstrap

echo %script_prefix% Unzipping distribution files

REM look for zip file
echo %intermediate_prefix% Looking in %dist_origin%\
set zip_file=
for /r %%i in (*.zip) do set zip_file=%%i
if "!zip_file!"=="" (
  echo %error_prefix% No distribution zip files found in %dist_origin%\!
  POPD
  exit /b %errorlevel%
)
echo %intermediate_prefix% Found zip file !zip_file!

REM check directory
if exist %dist_target%\ (
  echo %intermediate_prefix% Clearing directory %dist_target%\
  del /s /f /q %dist_target%\*.* > nul
  for /f %%f in ('dir /ad /b %dist_target%\') do rd /s /q %dist_target%\%%f
)

REM unzip
echo %intermediate_prefix% Unzipping to %dist_target%\..
call powershell Expand-Archive !zip_file! -DestinationPath %dist_target%\ > nul
echo %intermediate_prefix% Finished unzipping to %dist_target%\

REM move to root
PUSHD %dist_target%
set root_folder=
for /d %%D in (*) do set root_folder=%%~nxD
echo %intermediate_prefix% Moving files from %dist_target%\!root_folder!\ to %dist_target%\
REM move root files
for /r %%i in ("!root_folder!\*") do move %%i %%~pi.. > nul
REM move subdirectories
PUSHD !root_folder!
for /d %%D in (*) do move %%D ../%%D > nul
POPD
REM remove old parent
@RD /S /Q "!root_folder!"

echo %intermediate_prefix% Removing batch files from %dist_target%\%start_folder%
PUSHD %start_folder%
del /s /q /f *.bat > nul
echo %intermediate_prefix% Renaming start script in %dist_target%\%start_folder%
ren * start > nul
POPD
POPD

echo %script_prefix% Preparing docker environment
echo %intermediate_prefix% Copying %deploy_root%\%start_script% to %dist_target%\%start_script%
copy "%deploy_root%\%start_script%" "%dist_target%\%start_script%" > nul

echo %script_prefix% Building docker image
PUSHD %deploy_root%
call docker build -t "%image_name%" .
POPD

REM exit
echo %finished_prefix% Finished
POPD
