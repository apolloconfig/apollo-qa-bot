(function () {
  const QABot = {
    settings: {
      serverUrl: '',
      documentSiteUrlPrefix: ''
    },
    initialize: function (config) {
      this.settings.serverUrl = config.serverUrl;
      this.settings.documentSiteUrlPrefix = config.documentSiteUrlPrefix;
      this.render();
    },
    render: function () {

      // Create HTML structure
      let botIcon = document.createElement('div');
      botIcon.id = 'qa-bot-icon';
      botIcon.innerHTML = QABot.chatIconSvg;

      let textContainer = document.createElement('div');
      let text = document.createElement('span');
      text.textContent = 'Chat with AI';

      textContainer.appendChild(text);
      botIcon.appendChild(textContainer);

      document.body.appendChild(botIcon);

      let popupContainer = document.createElement('div');
      popupContainer.id = 'qa-bot-popup-container';
      document.body.appendChild(popupContainer);

      let botPopup = document.createElement('div');
      botPopup.id = 'qa-bot-popup';
      popupContainer.appendChild(botPopup);

      let botMessageList = document.createElement('div');
      botMessageList.id = 'qa-bot-message-list';
      botPopup.appendChild(botMessageList);

      let botMessage = document.createElement('div');
      botMessage.classList.add('qa-bot-message');
      botMessage.innerHTML = '<div class="qa-bot-message-icon">' + QABot.botIconSvg
          + '</div><div class="qa-bot-message-text">Hi, how can I help you?</div>';
      botMessageList.appendChild(botMessage);

      let userInput = document.createElement('div');
      userInput.id = 'qa-bot-user-input';
      botPopup.appendChild(userInput);

      let inputField = document.createElement('input');
      inputField.type = 'text';
      inputField.id = 'qa-bot-question-input';
      inputField.placeholder = 'Type your question here...';
      userInput.appendChild(inputField);

      let submitButton = document.createElement('button');
      submitButton.id = 'qa-bot-submit-button';
      submitButton.textContent = 'Ask';
      userInput.appendChild(submitButton);

      let loadingIcon = document.createElement('div');
      loadingIcon.id = 'qa-bot-loading-icon';
      userInput.appendChild(loadingIcon);

      // Create "Powered by" message
      let poweredBy = document.createElement('div');
      poweredBy.id = 'qa-bot-powered-by';
      poweredBy.innerHTML = 'Powered by ' + QABot.githubIconSvg;

      let poweredByLink = document.createElement('a');
      poweredByLink.href = 'https://github.com/apolloconfig/apollo-qa-bot';
      poweredByLink.textContent = 'apollo-qa-bot';
      poweredByLink.target = '_blank';

      poweredBy.appendChild(poweredByLink);
      botPopup.appendChild(poweredBy);

      // Register event listeners
      botIcon.addEventListener('click', function () {
        popupContainer.style.display = 'flex';
      });

      popupContainer.addEventListener('click', function (event) {
        if (event.target === popupContainer) {
          popupContainer.style.display = 'none';
        }
      });

      document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
          popupContainer.style.display = 'none';
        }
      });

      inputField.addEventListener('keyup', function (event) {
        if (event.key === 'Enter') {
          event.preventDefault();
          submitButton.click();
        }
      });

      submitButton.addEventListener('click', function () {
        let question = inputField.value;
        QABot.askQuestion(question);
        inputField.value = '';
        submitButton.style.display = 'none';
        loadingIcon.style.display = 'block';
      });

    },
    askQuestion: function (question) {
      if (question.trim() === '') {
        alert('Please input your question.');
        return;
      }

      // Add question to chat history
      let userMessage = document.createElement('div');
      userMessage.classList.add('qa-bot-user-message');
      userMessage.innerHTML = '<div class="qa-bot-user-message-icon">' + QABot.userIconSvg
          + '</div><div class="qa-bot-user-message-text">'
          + question + '</div>';
      document.getElementById('qa-bot-message-list').appendChild(userMessage);

      // Scroll to bottom
      document.getElementById('qa-bot-message-list').scrollTop = document.getElementById(
          'qa-bot-message-list').scrollHeight;

      // Call API
      fetch(this.settings.serverUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'question=' + encodeURIComponent(question)
      })
      .then(response => response.json())
      .then(data => {
        // Add answer to chat history
        let botMessage = document.createElement('div');
        botMessage.classList.add('qa-bot-message');
        botMessage.innerHTML = '<div class="qa-bot-message-icon">' + QABot.botIconSvg
            + '</div><div class="qa-bot-message-text">'
            + data.answer + '</div>';
        document.getElementById('qa-bot-message-list').appendChild(botMessage);

        // If there are related files, add them to chat history
        if (data.relatedFiles && data.relatedFiles.length > 0) {
          let relatedFiles = document.createElement('div');
          relatedFiles.classList.add('qa-bot-related-files');
          relatedFiles.innerHTML = '<div class="qa-bot-related-files-title">Related Docs:</div>';
          const fileList = document.createElement('ul');
          data.relatedFiles.forEach(function (file) {
            const fileItem = document.createElement('li');
            let fileLink = document.createElement('a');
            fileLink.href = QABot.settings.documentSiteUrlPrefix + file;
            fileLink.target = '_blank';
            fileLink.textContent = file;
            fileItem.appendChild(fileLink);
            fileList.appendChild(fileItem);
          });
          relatedFiles.appendChild(fileList);
          document.getElementById('qa-bot-message-list').appendChild(relatedFiles);
        }

        // Scroll to bottom
        document.getElementById('qa-bot-message-list').scrollTop = document.getElementById(
            'qa-bot-message-list').scrollHeight;

      })
      .catch(error => {
        alert('Error:' + error);
      })
      .finally(() => {
        // Re-enable submit button and hide loading icon
        document.getElementById('qa-bot-submit-button').style.display = 'block';
        document.getElementById('qa-bot-loading-icon').style.display = 'none';
      });
    },
    botIconSvg: '<?xml version="1.0" encoding="UTF-8" standalone="no"?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="40px" height="37px" viewBox="0 0 40 37" enable-background="new 0 0 40 37" xml:space="preserve">  <image id="image0" width="40" height="37" x="0" y="0"    href="data:image/png;base64,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" /></svg>',
    userIconSvg: '<?xml version="1.0" encoding="UTF-8" standalone="no"?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="32px" height="32px" viewBox="0 0 32 32" enable-background="new 0 0 32 32" xml:space="preserve">  <image id="image0" width="32" height="32" x="0" y="0"    href="data:image/png;base64,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" /></svg>',
    chatIconSvg: '<?xml version="1.0" encoding="UTF-8" standalone="no"?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="50px" height="50px" viewBox="0 0 50 50" enable-background="new 0 0 50 50" xml:space="preserve">  <image id="image0" width="50" height="50" x="0" y="0"    href="data:image/png;base64,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BUfl8ju0XINPvGY4xv+nNofu8b4RTzpo1My1qPxqFUm52suec8qbSTp2OWv9XCWdRJMD+l0bM5JdopQ7/eC7qkn+tNtyq+KNKVlD5aKjIgwJLEgiqV0+JPqZl4wPS9rXuF9dyTsFAsd0GqsVyTrbpWPGe8QBf9WK5Gs9MjPuCgw4U7vpOlODcI6ZiHlAvPCV4if7yyZAqlLh45H2pe/LxFyDVucqEz4Jv5cN6w3W6Lo0wtdbIdRgna0GvRtW7zEGm3SoU6/Rr17RXY3wgR8ThF+p1V69ftHlRK2Fn9rIzutweKK7lOQrPTIVr8u90yjS1tld2MSnHTdKSYbnLHwk8rQB21wsNU2cRqpHIG/EVi+YsbOELmhWV4O9cbm8do962+mZxn3OuKtqimW69Wlzo98MOeyc24rNG5x21IOWWTRZFpZolbbQYos1adKsWYsWSyyx1FJLtVprj7zXwVq9IiPFJIHAUzoscsK/k705H9kx2ar3qkcKR70gpg61YKe3jBRe+2NXuHsqhMdFv9lGN8kWBGtwqy9sLrTStmp33EnjETReiq9z+nTaXxTOG633hoGZs5qRlZVV5zonfTYZc1uMOmBl4YNt6vNt4qMujE3esvvUVU74vEDyuL/9nHRoqwZTJPcZdMwt1TpMHlbkdjvkbHGkWpKkpT5ntQ8t8Kxvaz9V49PV77Bu/3jm0hCMk/SJjHqxRqta4kjytv2Pvb4M4jQ565B+HXM/+1aGy2s3innDf3ce+rRU60aYAAAAXGVYSWZNTQAqAAAACAAEAQYAAwAAAAEAAgAAARIAAwAAAAEAAQAAASgAAwAAAAEAAQAAh2kABAAAAAEAAAA+AAAAAAACoAIABAAAAAEAAAIAoAMABAAAAAEAAAIAAAAAAJMPbVgAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjMtMDYtMjBUMTI6MDI6MTcrMDA6MDCMr+p2AAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIzLTA2LTIwVDEyOjAyOjE3KzAwOjAw/fJSygAAACh0RVh0ZGF0ZTp0aW1lc3RhbXAAMjAyMy0wNi0yMFQxMjowMjoxNyswMDowMKrncxUAAAASdEVYdGV4aWY6RXhpZk9mZnNldAA2MjAao3gAAAAgdEVYdGV4aWY6UGhvdG9tZXRyaWNJbnRlcnByZXRhdGlvbgAyooyJKwAAABh0RVh0ZXhpZjpQaXhlbFhEaW1lbnNpb24ANTEyti643AAAABh0RVh0ZXhpZjpQaXhlbFlEaW1lbnNpb24ANTEyKyFZqgAAABV0RVh0ZXhpZjpSZXNvbHV0aW9uVW5pdAAyjirYfAAAACh0RVh0aWNjOmNvcHlyaWdodABDb3B5cmlnaHQgQXBwbGUgSW5jLiwgMjAyM5OzjwoAAAAXdEVYdGljYzpkZXNjcmlwdGlvbgBEaXNwbGF5FxuVuAAAABJ0RVh0dGlmZjpDb21wcmVzc2lvbgAx2VmtcwAAABJ0RVh0dGlmZjpPcmllbnRhdGlvbgAxt6v8OwAAACB0RVh0dGlmZjpQaG90b21ldHJpY0ludGVycHJldGF0aW9uADIjwjCQAAAAFXRFWHR0aWZmOlJlc29sdXRpb25Vbml0ADKcKk+jAAAAF3RFWHR4bXA6UGl4ZWxYRGltZW5zaW9uADUxMqwh9VEAAAAXdEVYdHhtcDpQaXhlbFlEaW1lbnNpb24ANTEyMS4UJwAAAABJRU5ErkJggg==" /></svg>',
    githubIconSvg: '<?xml version="1.0" encoding="UTF-8" standalone="no"?><!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd"><svg width="16" height="16" aria-hidden="true"><path fill-rule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"></path></svg>',
  };

  // Expose QABot to global scope
  window.QABot = QABot;
})();
