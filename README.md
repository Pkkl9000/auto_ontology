Ontology Builder with LLM
Простой пример создания онтологии (структуры понятий) с применением LLM

Запускается через AppRunner

Модель LLM задаётся в LLMAssistant

Консоль базы (H2):
http://localhost:8084/h2-console  
application.properties:  
server.port=8084  
spring.datasource.url=jdbc:h2:file:./data/ontology  
Дополнительный сервис со скриптом для общения со SpaCy:  
https://github.com/Pkkl9000/SpaCy_script
