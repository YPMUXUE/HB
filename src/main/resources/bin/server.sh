#!/usr/bin/env bash
cd ..
APP_DIR=`pwd`
echo 'APP_DIR:'${APP_DIR}

LOG_DIR=${APP_DIR}/log
echo 'LOG_DIR:'${LOG_DIR}

CONF_DIR=${APP_DIR}/conf
echo 'CONF_DIR:'${CONF_DIR}

LIB_DIR=${APP_DIR}/lib
echo 'LIB_DIR:'${LIB_DIR}

if [[ -z $1 ]]; then
MAIN_CLASS='priv.Server.ProxyServer'
else
MAIN_CLASS=$1
fi

CLASSPATH=`ls ${LIB_DIR} | awk '/^.*\.jar/{jars[NR]=$0}END{list="";for(i in jars){if(list!=""){list=list":"}list=list"'${LIB_DIR}'/"jars[i];}print list;}'`

nohup java -DAppName=HB -classpath ${APP_DIR}:${CONF_DIR}:${CLASSPATH} ${MAIN_CLASS} > ${LOG_DIR}/stdout.log 2>&1 &

