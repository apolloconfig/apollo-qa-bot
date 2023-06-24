#!/bin/sh
#
# Copyright 2023 Apollo Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# open ai key info
openai_api_key='sk-xxxxx'
markdown_files_location='/user/xxx/docs'
doc_site_domain='https://docs.xxx.com'

# =============== Please do not modify the following content =============== #
# go to script directory
cd "${0%/*}" || exit 

echo "==== starting to build qa-bot ===="

mvn clean package -DskipTests -Dopenai_api_key=$openai_api_key -Dmarkdown_files_location=$markdown_files_location -Ddoc_site_domain=$doc_site_domain

echo "==== building qa-bot finished ===="
