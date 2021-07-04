# ToDo Application

Aplikacja do zarządzania zadaniami

## Zadanie

Przedmiotem zadania jest zaimplementowanie aplikacji internetowej do zarządzania zadaniami. 
Aplikacja umożliwia tworzenie użytkowników oraz zarządzanie prostymi zadaniami (tworzenie, wyświetlanie, usuwanie, ...)

### Wymagania

1. Aplikacja powinna udostępniać zasoby i funkcjonalność poprzez REST API
1. Aplikacja powinna być dostępona pod adresem [http://localhost:8080](http://localhost:8080/)
1. Aplikacja powinna obsługiwać treści żądań w formacie JSON
1. Aplikacja powinna zwracać treści odpowiedzi w formacie JSON
1. Aplikacja powinna obsługiwać następujące żądania HTTP
    * _metoda_ - metoda HTTP żądania,
    * _adres_ - ścieżka adresu URL żądanego zasobu,
    * _nagłówki_ - zmienne wysyłane w nagłówku żądania,
    * _parametry_ - zmienne wysyłane w ścieżce zasobu,
    * _treść_ - przykład ewentualnej treści żądania,
    * _odpowiedzi_ - obsługiwane kody statusu oraz przykład ewentualnej treści odpowiedzi
  
    metoda | adres | nagłówki | parametry | ciało | odpowiedzi
    ------ | ----- | -------- | --------- | ----- | ----------
    POST | /todo/user | | | <pre>{<br/>&#9;"username": "janKowalski",<br/>&#9;"password": "am!sK#123"<br/>}</pre> | <ul> <li>201</li><li>400</li><li>409</li> </ul>
    POST | /todo/task | auth | | <pre>{<br/>&#9;"description": "Kup mleko",<br/>&#9;"due": "2021-06-30"<br/>}</pre> | <ul><li>201<pre>{<br/>&#9;"id": "237e9877-e79b-12d4-a765-321741963000"<br/>}</li><li>400</li><li>401</li><ul>
    GET | /todo/task | auth | | | <ul><li>200<pre>[<br/>&#9;{<br/>&#9;&#9;"id": "237e9877-e79b-12d4-a765-321741963000",<br/>&#9;&#9;"description": "Kup mleko",<br/>&#9;&#9;"due": "2021-06-30"<br/>&#9;}<br/>]</pre></li><li>400</li><li>401</li></ul>
    GET | /todo/task/{id} | auth | id | | <ul><li>200<pre>{<br/>&#9;"id": "237e9877-e79b-12d4-a765-321741963000",<br/>&#9;"description": "Kup mleko",<br/>&#9;"due": "2021-06-30"<br/>}</pre></li><li>400</li><li>401</li><li>403</li><li>404</li></ul>
    PUT | /todo/task/{id} | auth | id | <pre>{<br/>&#9;"description": "Kup mleko",<br/>&#9;"due": "2021-06-30"<br/>}</pre> | <ul><li>200<pre>{<br/>&#9;"id": "237e9877-e79b-12d4-a765-321741963000",<br/>&#9;"description": "Kup mleko",<br/>&#9;"due": "2021-06-30"<br/>}</pre></li><li>400</li><li>401</li><li>403</li><li>404</li></ul>
    DELETE | /todo/task/{id} | auth | id |  | <ul><li>200</li><li>400</li><li>401</li><li>403</li><li>404</li></ul>
  
    * **auth** - ciąg znaków 'base64(username):base64(password)', gdzie base64() oznacza funkcję kodującą algorytmem Base64. Np., dla
    użytkownika `{ "username": "janKowalski", "password": "am!sK#123" }`, `auth` będzie równe `amFuS293YWxza2k=:YW0hc0sjMTIz`
    * **id** - unikalny identyfikator zadania w formacie UUID

    :warning: Informacje o tym, które nagłówki, parametry lub pola w dokumentach JSON są wymagane, znajdują się w
    szczegółowej [dokumenatcji Swagger API](https://epam-online-courses.github.io/efs-task9-todo-app/) aplikacji

1. Aplikacja powinna zwracać odpowiedź z kodem `404` (HTTP _Not Found_) dla nieobsługiwanych adresów
1. Zadania i użytkownicy stworzeni przy użyciu żądań, powinni być pamiętane w ramach jednokrotnego uruchomienia aplikacji
1. Implementacja powinna zawierać testy jednostkowe sprawdzające wszystkie obsługiwane żądania
1. Testy jednostkowe powinny sprawdzać wszystkie scenariusze użycia wyspecyfikowane w dokumentacji (zwracanie wszystkich możliwych odpowiedzi)
1. Aplikacja powinna logować najważniejsze informacje w trakcie działania aplikacji.
