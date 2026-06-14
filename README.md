# KryptoChat
Aplikacja do czatów z szyfrowaniem end-to-end w Javie - projekt
Projekt został ukończony.

Projekt realizowany w ramach przedmiotu Java. To repozytorium, to podprojekt, obsługujący serwerową część aplikacji.
Zespół projektowy:
1. Łukasz Deja
2. Natalia Parszywka

## Technologie

- Java 21
- Spring Boot 3.3.5
- Spring Data JPA
- Spring Security
- Spring WebSocket
- PostgreSQL
- JWT Authentication
- Maven

Aplikacji serwerowej nie ma potrzeby uruchamiać. Działa ona w trybie ciągłym na hostingu railway pod adresem:
https://kryptochatserwer-production.up.railway.app/
Gdzie można zobaczyć informację o tym, że aplikacja działa. Hosting deployuje najnowszy commit z tego repozytorium.
Hosting uruchamia aplikacją na JDK 21.0.2, używa PostgreSQL 18.3. W przypadku chęci uruchomienia aplikacji serwerowej lokalnie, konieczne jest skonfigurowanie bazy danych oraz utworzenie zmiennych środowiskowych wykorzystywanych przez JDK: DATABASE_URL, PGUSER, PGPASSWORD z odpowiednimi wartosciami. Absolutnie konieczne jest również ustawienie zmiennej środowiskowej JWT_SECRET - ciąg co najmniej 32 znaków, gdyż serwis tokenów JWT korzysta z niej przy tworzeniu tokenów. Aplikacja jest zbudowana na frameworku Spring Boot w wersji 3.3.5. W przypadku chęci uruchomienia jej lokalnie, należy uruchamiać poprzez mavena korzystając z pluginu spring-boot:run (podobnie jak na kliencie javafx:run) lub z konsoli mvn spring-boot:run. Uruchamianie aplikacji lokalnie NIE JEST jednak zalecane, wskazane jest żeby tego nie robić, gdyż adresy endpointów oraz websocketa są skonfigurowane na kliencie tak aby łączyć się z adresem hostingowym, a aplikacja na HOSTINGU w wersji produkcyjnej DZIAŁA W PEŁNI POPRAWNIE i realizuje wszystkie funkcjonalności.

Uruchomienie testów (NA JAVIE 21 TRZEBA):

ZALECANE jest jedynie uruchomienie testów jednostkowych oraz integracyjnych aplikacji serwera lokalnie. Plik test > resources > application.properties pozwala na uruchomienie testów jednostkowych (zignorowanie bazy danych), a plik test > resources > application-integration.properties pozwala na uruchomienie testów integracyjnych (Baza danych h2). Do uruchomienia testów konieczne jest do tego edytowanie Run Configuration testów, tak aby zawierała zmienną środowiskową JWT_SECRET zawierającą ciąg co najmniej 32 znaków, najlepiej liczb. Najłatwiej zrobić to w IntelliJ w następujący sposób:

1. W przypadku jeżeli maven nie zostanie rozpoznany, należy kliknąć PPM na plik pom.xml i wybrac opcję Add as Maven Project
2. Testy znajdują się w katalogu src > test > java. Podkatalog (pakiet) integration zawiera testy integracyjne, pozostałe zawierają testy jednostkowe.
3. Sugeruję kliknąć prawym przyciskiem myszy na katalog test > java i wybrać Run All tests po czym od razu wyłączyć testy. Na górze pojawi nam się możliwość edycji konfiguracji uruchomieniowej:

<img width="402" height="182" alt="image" src="https://github.com/user-attachments/assets/206706bf-9d67-48b7-af80-bfe2782f15bb" />

4. Wybrać Edit Configurations a tam ustawić, KONIECZNIE, java 21 oraz w polu Environmental Variables wpisać przykładowo: JWT_SECRET=12345678901234567890123456789012
Następnie apply oraz OK.

<img width="987" height="860" alt="image" src="https://github.com/user-attachments/assets/8de730f2-51f9-41e1-946c-e0e9d6ebd9fc" />

5. Po konfiguracji można uruchomić wszystkie testy ponownie - zielony trójkątny przycisk obok All in serwer na górze.
6. W przypadku chęci uruchomienia tylko części testów, na przykład konkretnego pliku lub pakietu, konieczne jest analogiczne ustawienie konfiguracji uruchomieniowej dla nich.


Uruchomienie testów serwera bez IntelliJ:
Tymczasowo przełączamy wersję Javy z 25(klient) na 21:
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

Eksportujemy zmienną środowiskową:
export JWT_SECRET=12345678901234567890123456789012
Uruchamiamy testy:
(w katalogu KryptoChatSerwer tam znajduje się pom.xml)
mvn test




