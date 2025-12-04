## Работа над ошибками Спринт 10:

* Убраны бинарные архивы Helm из репозитория.

* В Деплоймент добавлены корректные настройки resources: requests/limits, livenessProbe и readinessProbe.

* Исправлен Jenkins-пайплайн: тест → dev → test → prod: сделан правильный многоэтапный CI/CD с разделением namespace:

bank-dev

bank-test

bank-prod

* Исправлены Helm-тесты.

* Для prod добавлен единый Ingress Gateway. Приложение доступно по адресу: http://localhost/bank.

* К сожалению, не удалось корректно реализовать авторизацию пользователя и аутентификацию (получение токенов для микросервисов) Кейклок в K8s одновременно.

Keycloak внутри контейнера работает на внутреннем hostname (hostname внутри pod'а), а внешние сервисы (браузер) обращаются к нему через внешний URL (Ingress/NodePort), из-за чего происходит несоответствие redirect_uri и issuer.

OIDC Discovery (/.well-known/openid-configuration) отдает внутренние URL, типа: http://keycloak:8080/realms/bank, а браузер- по внешним URL, например: http://auth.localhost/realms/bank

Keycloak запрещает авторизацию и выдает ошибки вида: Invalid redirect_uri, Issuer mismatch, Token validation failed.

- В настоящий момент найден один из путей реализации данной конфигурации приложения: использование частично динамических URL.

При этом: запускаем Keycloak с явным --hostname (наружный URL):

    kc.sh start \
    --hostname http://auth.localhost \
    --proxy-headers xforwarded \
    --http-enabled true \
    --hostname-strict=true

тогда:

OIDC discovery (/.well-known/openid-configuration) всегда отдаёт issuer = http://auth.localhost/realms/bank,

все эндпоинты (auth, token, jwks) будут тоже на http://auth.localhost/....

В связи с ограничением по времени, это решение пока не применено в проекте.

