# qa-bot

A smart qa bot that can answer questions based on existing documents.

## Getting Started

### Prerequisites

1. Java 1.8+
2. Milvus 2.2.0+

### Installation

1. Clone this repo
2. Execute `./build.sh`
3. Upload and unzip the generated `qa-bot-x.x.x.jar` to the server

### Configuration

#### Edit the `config/application.yaml`

1. Config the `markdown.files.location` to the directory of the markdown files
2. Config the `markdown.files.scheduleEnabled` to `true` if you want to auto update the markdown files
3. Config the `milvus.host` and `milvus.port` to the Milvus server
4. Config the `milvus.useZillzCloud`, `milvus.zillizCloudUri` and `milvus.zillizCloudToken` if you are using Zilliz Cloud
5. Config other parameters as needed

#### Edit the `qa-bot.conf`

1. Config the `OPENAI_API_KEY` to the OpenAI API key
2. Config the `HTTP_PROXY` to the proxy server if needed

```bash
export OPENAI_API_KEY=sk-************************************************
export HTTP_PROXY=http{s}://${username}:${password}@${url}:${port}
```

### Usage

1. Start the service: `./scripts/startup.sh`
2. Stop the service: `./scripts/shutdown.sh`
3. Check the logs: `tail -f /opt/logs/qa-bot.log`
4. Manually trigger the markdown files processing: `curl http://${your-server-url}:9090/markdown/load`
5. Test the QA bot via browser: `http://${your-server-url}:9090`

#### Integrate the QA bot with your website

Refer [apollo pr](https://github.com/apolloconfig/apollo/pull/4908/) for an example.

```html

<html>
    <head>
      ...
    </head>
    <body>
      ...
    </body>
    <!-- add qa bot -->
    <script>
      (function() {
        var link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = 'http://${your-server-url}:9090/qa-bot.css';
        document.head.appendChild(link);

        var script = document.createElement('script');
        script.src = 'http://${your-server-url}:9090/qa-bot.js';
        script.onload = function () {
          QABot.initialize({
            "serverUrl": "http://${your-server-url}:9090/qa",
            "documentSiteUrlPrefix": "https://${your-documentation-site-prefix-url}"
          });
        };
        document.body.appendChild(script);
      })();
    </script>
</html>
```