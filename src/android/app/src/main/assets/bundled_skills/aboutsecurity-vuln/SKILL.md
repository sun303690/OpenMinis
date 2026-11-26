---
name: aboutsecurity-vuln
description: "结构化漏洞库：664 条产品/CVE 条目（AI 应用、云、中间件 437 条、网络设备、Web 应用），每条含影响版本、指纹、验证步骤与 PoC。按产品名 grep 或搜索即可定位到具体 CVE，用于确认目标版本是否受影响、复现路径与利用细节。"
category: 漏洞库
author: wgpsec
source: https://github.com/wgpsec/AboutSecurity/tree/main/Vuln
version: "1.0.0"
---

# AboutSecurity 漏洞库

共 **664** 条条目，其中 CRITICAL/HIGH 603 条。

## 使用方式

条目正文在 `vuln/<分类>/<产品>/<编号>.md`，含影响版本、指纹、验证步骤与 PoC。
**按产品定位**（比逐条读快得多）：

```
grep -ril "<产品名>" /builtin_skills/aboutsecurity-vuln/vuln/ | head -20
```

或用 file 工具的 search 动作在 `/builtin_skills/aboutsecurity-vuln/vuln/` 下搜产品关键词、
CVE 编号、指纹串（如 `Nacos-Server`）。每条 frontmatter 都有：
`id / title / product / vendor / version_affected / severity / tags / fingerprint`。

## 分类与产品索引

### ai（15 个产品 / 24 条）

| 产品 | 条目 | 严重度 |
|---|---|---|
| `anythingllm` | 1 | CRITICAL×1 |
| `browser-use` | 1 | CRITICAL×1 |
| `comfyui` | 2 | CRITICAL×1 HIGH×1 |
| `dify` | 3 | CRITICAL×2 MEDIUM×1 |
| `flowise` | 1 | CRITICAL×1 |
| `label-studio` | 1 | HIGH×1 |
| `langflow` | 4 | CRITICAL×3 HIGH×1 |
| `localai` | 1 | CRITICAL×1 |
| `ollama` | 4 | CRITICAL×2 HIGH×2 |
| `opencode` | 1 | CRITICAL×1 |
| `openwebui` | 1 | HIGH×1 |
| `qdrant` | 1 | HIGH×1 |
| `ragflow` | 1 | CRITICAL×1 |
| `text-generation-webui` | 1 | CRITICAL×1 |
| `vllm` | 1 | HIGH×1 |

### cloud（4 个产品 / 5 条）

| 产品 | 条目 | 严重度 |
|---|---|---|
| `aws-apigateway` | 1 | HIGH×1 |
| `aws-lambda` | 1 | HIGH×1 |
| `aws-s3` | 2 | HIGH×1 MEDIUM×1 |
| `aws-sns` | 1 | HIGH×1 |

### middleware（151 个产品 / 436 条）

| 产品 | 条目 | 严重度 |
|---|---|---|
| `activemq` | 5 | CRITICAL×3 HIGH×1 MEDIUM×1 |
| `airflow` | 3 | CRITICAL×2 HIGH×1 |
| `apisix` | 4 | CRITICAL×3 HIGH×1 |
| `appweb` | 1 | HIGH×1 |
| `argocd` | 2 | CRITICAL×1 HIGH×1 |
| `aria2` | 1 | CRITICAL×1 |
| `artifactory` | 1 | CRITICAL×1 |
| `bash` | 1 | CRITICAL×1 |
| `celery` | 1 | CRITICAL×1 |
| `cgi` | 1 | MEDIUM×1 |
| `clickhouse` | 1 | HIGH×1 |
| `coldfusion` | 5 | CRITICAL×4 MEDIUM×1 |
| `confluence` | 6 | CRITICAL×5 MEDIUM×1 |
| `couchdb` | 3 | CRITICAL×2 HIGH×1 |
| `crowd` | 1 | CRITICAL×1 |
| `django` | 6 | HIGH×4 MEDIUM×2 |
| `docker` | 4 | CRITICAL×3 HIGH×1 |
| `dubbo` | 2 | CRITICAL×2 |
| `elasticsearch` | 4 | CRITICAL×2 HIGH×2 |
| `erlang-otp` | 1 | CRITICAL×1 |
| `exchange` | 2 | CRITICAL×2 |
| `f5-bigip` | 3 | CRITICAL×3 |
| `fastjson` | 3 | CRITICAL×2 HIGH×1 |
| `flask` | 1 | CRITICAL×1 |
| `flink` | 2 | CRITICAL×1 MEDIUM×1 |
| `geoserver` | 5 | CRITICAL×3 HIGH×2 |
| `ghostscript` | 3 | CRITICAL×3 |
| `git` | 1 | CRITICAL×1 |
| `gitea` | 1 | CRITICAL×1 |
| `gitlab` | 4 | CRITICAL×1 HIGH×1 MEDIUM×2 |
| `gitlist` | 1 | CRITICAL×1 |
| `glassfish` | 1 | MEDIUM×1 |
| `goahead` | 2 | CRITICAL×2 |
| `gogs` | 2 | CRITICAL×1 HIGH×1 |
| `gradio` | 2 | MEDIUM×2 |
| `grafana` | 8 | CRITICAL×2 HIGH×3 MEDIUM×3 |
| `h2database` | 3 | CRITICAL×3 |
| `hadoop` | 1 | CRITICAL×1 |
| `harbor` | 3 | CRITICAL×1 HIGH×1 MEDIUM×1 |
| `hertzbeat` | 1 | CRITICAL×1 |
| `httpd` | 7 | CRITICAL×4 HIGH×1 MEDIUM×2 |
| `hugegraph` | 2 | CRITICAL×2 |
| `iis` | 3 | CRITICAL×1 HIGH×1 MEDIUM×1 |
| `imagemagick` | 3 | CRITICAL×2 MEDIUM×1 |
| `inetutils` | 1 | CRITICAL×1 |
| `influxdb` | 1 | HIGH×1 |
| `ingress-nginx` | 1 | CRITICAL×1 |
| `inspur` | 1 | CRITICAL×1 |
| `jackson` | 3 | CRITICAL×3 |
| `java` | 4 | CRITICAL×4 |
| `jboss` | 4 | CRITICAL×4 |
| `jenkins` | 8 | CRITICAL×6 HIGH×2 |
| `jetty` | 3 | MEDIUM×3 |
| `jimureport` | 3 | CRITICAL×2 HIGH×1 |
| `jira` | 7 | CRITICAL×1 HIGH×3 MEDIUM×3 |
| `jmeter` | 1 | CRITICAL×1 |
| `jumpserver` | 2 | CRITICAL×1 HIGH×1 |
| `jupyter` | 1 | CRITICAL×1 |
| `kafka` | 1 | CRITICAL×1 |
| `kibana` | 4 | CRITICAL×3 MEDIUM×1 |
| `kkfileview` | 1 | CRITICAL×1 |
| `kong` | 2 | CRITICAL×2 |
| `kong-insomnia` | 1 | CRITICAL×1 |
| `konga` | 1 | HIGH×1 |
| `kubernetes` | 3 | CRITICAL×3 |
| `kylin` | 1 | CRITICAL×1 |
| `laravel` | 3 | CRITICAL×1 HIGH×2 |
| `librsvg` | 1 | MEDIUM×1 |
| `libssh` | 1 | CRITICAL×1 |
| `liferay-portal` | 1 | CRITICAL×1 |
| `livewire` | 1 | CRITICAL×1 |
| `log4j` | 2 | CRITICAL×2 |
| `magento` | 1 | HIGH×1 |
| `metersphere` | 2 | CRITICAL×1 HIGH×1 |
| `mini_httpd` | 1 | MEDIUM×1 |
| `minio` | 3 | HIGH×2 MEDIUM×1 |
| `mojarra` | 1 | CRITICAL×1 |
| `mongo-express` | 1 | CRITICAL×1 |
| `mssql` | 4 | CRITICAL×2 HIGH×2 |
| `mysql` | 2 | CRITICAL×1 HIGH×1 |
| `n8n` | 3 | CRITICAL×3 |
| `nacos` | 7 | CRITICAL×6 HIGH×1 |
| `nagios` | 1 | CRITICAL×1 |
| `neo4j` | 1 | CRITICAL×1 |
| `next.js` | 1 | CRITICAL×1 |
| `nexus` | 6 | CRITICAL×4 HIGH×2 |
| `nginx` | 7 | CRITICAL×2 HIGH×2 MEDIUM×3 |
| `nginx-ui` | 2 | CRITICAL×2 |
| `node` | 2 | CRITICAL×1 MEDIUM×1 |
| `nodejs` | 2 | CRITICAL×1 HIGH×1 |
| `nostromo` | 1 | CRITICAL×1 |
| `nuxeo` | 1 | CRITICAL×1 |
| `ofbiz` | 11 | CRITICAL×10 HIGH×1 |
| `openam` | 1 | CRITICAL×1 |
| `openfire` | 1 | HIGH×1 |
| `opensmtpd` | 1 | CRITICAL×1 |
| `openssh` | 1 | MEDIUM×1 |
| `openssl` | 2 | CRITICAL×1 MEDIUM×1 |
| `opentsdb` | 2 | CRITICAL×2 |
| `owncloud` | 1 | HIGH×1 |
| `pdfjs` | 1 | HIGH×1 |
| `pgadmin` | 4 | CRITICAL×4 |
| `php` | 10 | CRITICAL×6 HIGH×3 MEDIUM×1 |
| `phpmailer` | 1 | MEDIUM×1 |
| `phpmyadmin` | 3 | CRITICAL×2 HIGH×1 |
| `phpunit` | 1 | CRITICAL×1 |
| `polkit` | 1 | CRITICAL×1 |
| `portainer` | 4 | CRITICAL×3 MEDIUM×1 |
| `postgres` | 5 | CRITICAL×2 HIGH×3 |
| `pydash` | 1 | CRITICAL×1 |
| `python` | 4 | CRITICAL×3 MEDIUM×1 |
| `rabbitmq` | 4 | CRITICAL×2 HIGH×2 |
| `rails` | 3 | CRITICAL×1 MEDIUM×2 |
| `react` | 1 | CRITICAL×1 |
| `redis` | 3 | CRITICAL×3 |
| `rocketchat` | 1 | HIGH×1 |
| `rocketmq` | 2 | CRITICAL×2 |
| `roxy-wi` | 1 | CRITICAL×1 |
| `rsync` | 1 | CRITICAL×1 |
| `ruby` | 1 | CRITICAL×1 |
| `saltstack` | 5 | CRITICAL×3 HIGH×1 MEDIUM×1 |
| `samba` | 1 | CRITICAL×1 |
| `scrapy` | 1 | CRITICAL×1 |
| `shardingsphere` | 1 | CRITICAL×1 |
| `shiro` | 6 | CRITICAL×3 HIGH×3 |
| `showdoc` | 2 | CRITICAL×1 HIGH×1 |
| `skywalking` | 2 | HIGH×2 |
| `solr` | 11 | CRITICAL×8 HIGH×3 |
| `sonarqube` | 1 | HIGH×1 |
| `spark` | 3 | CRITICAL×3 |
| `spring` | 13 | CRITICAL×8 HIGH×5 |
| `spring-boot` | 1 | HIGH×1 |
| `spring-cloud` | 1 | CRITICAL×1 |
| `struts2` | 23 | CRITICAL×22 HIGH×1 |
| `superset` | 2 | CRITICAL×2 |
| `supervisor` | 1 | CRITICAL×1 |
| `teamcity` | 2 | CRITICAL×1 HIGH×1 |
| `tikiwiki` | 1 | CRITICAL×1 |
| `tomcat` | 8 | CRITICAL×5 HIGH×3 |
| `unomi` | 1 | CRITICAL×1 |
| `uwsgi` | 2 | HIGH×1 MEDIUM×1 |
| `v2board` | 1 | HIGH×1 |
| `vcenter` | 4 | CRITICAL×2 HIGH×1 MEDIUM×1 |
| `vite` | 2 | MEDIUM×2 |
| `vmware` | 3 | CRITICAL×2 HIGH×1 |
| `weblogic` | 25 | CRITICAL×20 HIGH×5 |
| `webmin` | 1 | CRITICAL×1 |
| `wso2` | 1 | CRITICAL×1 |
| `xstream` | 4 | CRITICAL×2 HIGH×2 |
| `yapi` | 2 | CRITICAL×2 |
| `zabbix` | 4 | CRITICAL×3 HIGH×1 |

### network（14 个产品 / 20 条）

| 产品 | 条目 | 严重度 |
|---|---|---|
| `anheng` | 1 | CRITICAL×1 |
| `cisco` | 3 | CRITICAL×2 HIGH×1 |
| `citrix` | 2 | CRITICAL×2 |
| `dahua` | 1 | CRITICAL×1 |
| `dns` | 1 | MEDIUM×1 |
| `fortinet` | 1 | CRITICAL×1 |
| `h3c` | 2 | CRITICAL×2 |
| `hikvision` | 1 | CRITICAL×1 |
| `huawei` | 1 | CRITICAL×1 |
| `nsfocus` | 2 | CRITICAL×2 |
| `ntopng` | 1 | HIGH×1 |
| `ruijie` | 2 | CRITICAL×1 HIGH×1 |
| `sangfor-ngaf` | 1 | CRITICAL×1 |
| `zeroshell` | 1 | CRITICAL×1 |

### web（81 个产品 / 179 条）

| 产品 | 条目 | 严重度 |
|---|---|---|
| `1panel` | 4 | CRITICAL×2 HIGH×2 |
| `adminer` | 2 | HIGH×2 |
| `aj-report` | 1 | CRITICAL×1 |
| `apache-cxf` | 1 | HIGH×1 |
| `apache-druid` | 3 | CRITICAL×2 HIGH×1 |
| `apereo-cas` | 1 | CRITICAL×1 |
| `budibase` | 2 | CRITICAL×1 HIGH×1 |
| `cacti` | 3 | CRITICAL×2 HIGH×1 |
| `chartbrew` | 1 | CRITICAL×1 |
| `cmsms` | 2 | CRITICAL×1 HIGH×1 |
| `coremail` | 1 | MEDIUM×1 |
| `craftcms` | 3 | CRITICAL×3 |
| `cups-browsed` | 1 | CRITICAL×1 |
| `dedecms` | 1 | CRITICAL×1 |
| `discuz` | 3 | CRITICAL×1 HIGH×1 MEDIUM×1 |
| `drupal` | 6 | CRITICAL×6 |
| `ecshop` | 1 | CRITICAL×1 |
| `epoint-oa` | 1 | MEDIUM×1 |
| `esafenet` | 1 | CRITICAL×1 |
| `eyou` | 1 | CRITICAL×1 |
| `finereport` | 6 | CRITICAL×3 HIGH×3 |
| `fuelcms` | 1 | CRITICAL×1 |
| `glpi` | 1 | CRITICAL×1 |
| `hexin` | 1 | CRITICAL×1 |
| `hongfan-oa` | 1 | HIGH×1 |
| `huatian-oa` | 1 | HIGH×1 |
| `huijietong` | 1 | HIGH×1 |
| `imo` | 1 | CRITICAL×1 |
| `jeecgboot` | 3 | CRITICAL×1 HIGH×2 |
| `jinher-oa` | 1 | HIGH×1 |
| `jixian-oa` | 1 | HIGH×1 |
| `joomla` | 3 | CRITICAL×1 HIGH×2 |
| `kingdee` | 5 | CRITICAL×3 HIGH×2 |
| `kingsoft-v8` | 1 | CRITICAL×1 |
| `landray-oa` | 5 | CRITICAL×5 |
| `lanproxy` | 1 | HIGH×1 |
| `leagsoft` | 2 | HIGH×2 |
| `metabase` | 2 | CRITICAL×1 MEDIUM×1 |
| `newcapec` | 1 | CRITICAL×1 |
| `nextjs` | 1 | CRITICAL×1 |
| `nfine` | 3 | MEDIUM×3 |
| `nocobase` | 1 | CRITICAL×1 |
| `opensns` | 1 | CRITICAL×1 |
| `pbootcms` | 1 | CRITICAL×1 |
| `phpcms` | 3 | CRITICAL×1 HIGH×1 MEDIUM×1 |
| `phpmoadmin` | 1 | CRITICAL×1 |
| `phpmyadmin` | 1 | HIGH×1 |
| `pigcms` | 1 | CRITICAL×1 |
| `qilai-oa` | 1 | HIGH×1 |
| `realfriend` | 1 | CRITICAL×1 |
| `ruoyi` | 2 | CRITICAL×1 HIGH×1 |
| `sangfor` | 2 | CRITICAL×2 |
| `seeyon-oa` | 9 | CRITICAL×7 HIGH×2 |
| `shiziyucms` | 1 | CRITICAL×1 |
| `shopxo` | 1 | HIGH×1 |
| `smartbi` | 1 | CRITICAL×1 |
| `smartoa` | 1 | HIGH×1 |
| `spiderflow` | 1 | CRITICAL×1 |
| `spring` | 1 | CRITICAL×1 |
| `sunlogin` | 1 | CRITICAL×1 |
| `taiji` | 1 | HIGH×1 |
| `thinkadmin` | 1 | HIGH×1 |
| `thinkphp` | 6 | CRITICAL×5 HIGH×1 |
| `tongda-oa` | 8 | CRITICAL×6 HIGH×2 |
| `topsec` | 1 | CRITICAL×1 |
| `trs` | 1 | CRITICAL×1 |
| `trs-wcm` | 2 | CRITICAL×1 HIGH×1 |
| `ueditor` | 2 | CRITICAL×1 HIGH×1 |
| `vbulletin` | 1 | CRITICAL×1 |
| `wanhu-oa` | 4 | CRITICAL×1 HIGH×3 |
| `weaver-oa` | 13 | CRITICAL×7 HIGH×4 MEDIUM×2 |
| `webmin` | 2 | CRITICAL×1 HIGH×1 |
| `weiphp` | 1 | HIGH×1 |
| `wework` | 1 | MEDIUM×1 |
| `wordpress` | 6 | CRITICAL×4 HIGH×2 |
| `xxl-job` | 3 | CRITICAL×2 HIGH×1 |
| `yimi-oa` | 1 | HIGH×1 |
| `yongyou` | 9 | CRITICAL×9 |
| `zentao` | 2 | CRITICAL×1 HIGH×1 |
| `zhixiang-oa` | 1 | HIGH×1 |
| `zzzcms` | 1 | CRITICAL×1 |
