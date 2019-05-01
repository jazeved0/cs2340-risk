@echo off
title image-build-script
setlocal enableDelayedExpansion
setlocal enableExtensions
PUSHD ..

REM command prefixes
set script_prefix=[92m[Image Script][0m
set intermediate_prefix=[93m[Image Script] -[0m
set error_prefix=[91m[Image Script][0m
set finished_prefix=[94m[Image Script][0m

REM parameters
set deploy_root=deploy
set deploy_path=svc
set dist_origin=target\universal
set dist_target=%deploy_root%\%deploy_path%
set start_folder=bin
set start_script=bootstrap
set dockerfile=%deploy_root%\Dockerfile
set temp_dir=%tmp%\image-build
set make_war=make-war.ps1
set transform-script=transform-docs.py
set result_file=results.txt
set docs_root=share\doc\api
set "options=--war:"" --image:"" --transform-docs:"

for %%O in (%options%) do for /f "tokens=1,* delims=:" %%A in ("%%O") do set "%%A=%%~B"
:loop
if not "%~1"=="" (
  set "test=!options:*%~1:=! "
  if "!test!"=="!options! " (
      echo Error: Invalid option %~1
  ) else if "!test:~0,1!"==" " (
      set "%~1=1"
  ) else (
      set "%~1=%~2"
      shift /1
  )
  shift /1
  goto :loop
)

if "!--war!"=="" (
  REM check for image param
  if "!--image!"=="" (
    echo %error_prefix% No image name given. Use argument --image [imagename] to to specify one
    POPD
    exit /b %errorlevel%
  )
) else (
  if not "!--image!"=="" (
    echo %error_prefix% Ignoring illegal argument "--image !--image!"; archiving to WAR instead
  )
)

REM start
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

echo %script_prefix% Preparing image environment
echo %intermediate_prefix% Copying %deploy_root%\%start_script% to %dist_target%\%start_script%
copy "%deploy_root%\%start_script%" "%dist_target%\%start_script%" > nul

if NOT "!--transform-docs!"=="" (
  PUSHD %dist_target%
  echo %intermediate_prefix% Transforming docs files
  py %~dp0%transform-script% %CD%\%dist_target%\%docs_root%
  POPD
)

if "!--war!"=="" (
  REM build docker image
  echo %script_prefix% Building docker image
  PUSHD %deploy_root%
  call docker build -t "!--image!" .
  POPD
) else (
  REM build war archive
  echo %script_prefix% Building war archive "!--war!"
  REM get unique file name
  :uniqLoop
  set "uniqueFileName=%temp_dir%\bat~%RANDOM%.tmp"
  if exist "!uniqueFileName!" goto :uniqLoop

  echo %intermediate_prefix% Copying %dist_target%\ to temp directory !uniqueFileName!\%deploy_path%\
  call mkdir !uniqueFileName!\%deploy_path% > nul
  call xcopy /e/y/i "%dist_target%" "!uniqueFileName!\%deploy_path%" > nul
  PUSHD !uniqueFileName!
  set total_before=0
  set total_after=0
  echo %intermediate_prefix% Building WAR archive...
  for /F "delims=" %%l in ('powershell -executionpolicy remotesigned -File %~dp0%make_war% -image !--war!') do (
    set flag=
    for %%n in (%%l) do (
      if "!flag!"=="" (
        set flag=true
        set /A total_before=!total_before! + %%n
      ) else (
        set /A total_after=!total_after! + %%n
      )
    )
  )
  set command_=powershell [math]::Round^(^(1 - ^(!total_after! ^/ !total_before!^)^) ^* 100, 2^)
  for /f %%x in ('!command_!') do set result=%%x
  echo %intermediate_prefix% Created WAR !--war! in temp folder with !result!%% deflation
  POPD
  echo %intermediate_prefix% Copying archive to %CD%\%deploy_root%\!--war!
  xcopy /y !uniqueFileName!\!--war! %CD%\%deploy_root% > nul
  echo %intermediate_prefix% Clearing temp folder
  @RD /S /Q !uniqueFileName! > nul
)

REM exit
echo %finished_prefix% Finished
POPD
