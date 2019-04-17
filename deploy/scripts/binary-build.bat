@echo off
title production-script
setlocal enabledelayedexpansion
cd ..

REM configurations
REM prod template to copy
set conf_template=conf\prod_template.conf
REM destination conf file for prod
set conf_target=conf\prod.conf
REM temp folder to put dist files
set temp_dist_dir=dist
REM folders to retain in the distribution
set "retain_dirs=vue\dist@dist,data\maps@data\maps,public@public,docs@share\doc"

REM command prefixes
set script_prefix=[92m[Build Script][0m
set intermediate_prefix=[93m[Build Script] -[0m
set finished_prefix=[94m[Build Script][0m

echo %script_prefix% Building front-end
cd vue
call npm runScript buildProd
cd ..

echo %script_prefix% Copying retainment directories
for %%a in (%retain_dirs%) do (
  set full_item=%%a
  set full_item=!full_item:@=,!
  set source_path=
  set target_path=
  for %%b in (!full_item!) do (
    if "!source_path!"=="" (
      set source_path=%%b
    ) else (
      set target_path=%%b
    )
  )
  xcopy /s/e/y/i "!source_path!" "%temp_dist_dir%\!target_path!" > nul
  echo %intermediate_prefix% Copied files from !source_path! to %temp_dist_dir%\!target_path!
)

echo %script_prefix% Configuring production environment
copy "%conf_template%" "%conf_target%" > nul
echo %intermediate_prefix% Created file %conf_target%

echo %script_prefix% Building back-end
call sbt dist

echo %script_prefix% Cleaning up
del %conf_target% > nul
echo %intermediate_prefix% Removed file %conf_target%
@RD /S /Q %temp_dist_dir% > nul
echo %intermediate_prefix% Removed folder %temp_dist_dir%\

echo %finished_prefix% Finished; look in [94m.\target\universal\[0m for the packaged archive
