#include <sys/socket.h>
#include <netinet/in.h>
#include <cstring>
#include <unistd.h>
#include <cstdio>
#include <map>
#include <pthread.h>
#include <sys/types.h>
#include <list>
#include <vector>
#include <bits/unique_ptr.h>

#define QUEUE_SIZE 5


struct ThreadData {         // <-- Struktura przekazywana do wątku ClientThread
    int client_descriptor;
    std::map<std::string, std::list<int>> *topicsClientsList;
    std::map<int, pthread_mutex_t> *clients_mutex;
    pthread_mutex_t *topics_clients_mutex;
};

void *clientThread(void *t_data)
{
    pthread_detach(pthread_self());
    ThreadData *client_data = (ThreadData*)t_data;

    while(true) {
        // Odczytujemy request od klienta, najpierw zapisujemy go do tablicy charów, a potem przepisujemy do stringa.
        char messageLength[4] = "";
        char request[1200] = "";

        // Odczytanie długości wiadomości
        int totalLength = 0;
        int currentLength = 0;
        std::string messageLengthString = "";
        while(totalLength < 4) {
            currentLength = read(client_data->client_descriptor, messageLength, 4);
            if(currentLength == -1) {
                printf("Błąd podczas odbierania długości wiadomości od klienta.");
                // zamknięcie socketu klienta i zwolnienie pamięci
                close(client_data->client_descriptor);
                delete client_data;
                client_data = nullptr;
                exit(-1);
            }
            totalLength += currentLength;
            messageLengthString.append(messageLength, 0, currentLength);
        }

        // Odczytanie wiadomości o danej długości
        int charsToRead = atoi(messageLengthString.c_str());
        int totalCount = 0;
        int currentCount = 0;
        std::string requestString = "";
        while(totalCount < charsToRead) {
            currentCount = read(client_data->client_descriptor, request, 1200);
            if(currentCount == -1) {
                printf("Błąd podczas odbierania wiadomości od klienta.");
                // zamknięcie socketu klienta i zwolnienie pamięci
                close(client_data->client_descriptor);
                delete client_data;
                client_data = nullptr;
                exit(-1);
            }
            totalCount += currentCount;
            requestString.append(request, 0, currentCount);
        }

        // Poszczególne części requestu odzielone są ciągami znaków "//divider@//", dzielimy więc otrzymaną wiadomość na części.
        // Zapisujemy do vectora, żeby łatwo było operować na poszczególnych elementach.
        // requestVector[0] przechowuje typ polecenia, czyli jedno z: SUBSCRIBE, UNSUBSCRIBE, SENDMESSAGE, LOGOUT
        std::string delimiter = "//divider@//";
        std::vector<std::string> requestVector;
        auto start = 0U;
        auto end = requestString.find(delimiter);
        while (end != std::string::npos) {
            requestVector.push_back(requestString.substr(start, end - start));
            start = end + delimiter.length();
            end = requestString.find(delimiter, start);
        }
        requestVector.push_back(requestString.substr(start, end));

        printf("\n------------------ REQUEST FROM CLIENT ------------------\n");
        for(const std::string& elem : requestVector) printf("---> %s\n", elem.c_str());
        printf("---------------------------------------------------------\n");

        if (requestVector[0] == "SUBSCRIBE") {
            // tutaj: requestVector[0] - nazwa tematu do zasubskrybowania
            pthread_mutex_lock(client_data->topics_clients_mutex);
            (*client_data->topicsClientsList)[requestVector[1]].push_back(client_data->client_descriptor);
            pthread_mutex_unlock(client_data->topics_clients_mutex);
        }

        else if (requestVector[0] == "UNSUBSCRIBE") {
            // tutaj: requestVector[0] - nazwa tematu do odsubskrybowania
            pthread_mutex_lock(client_data->topics_clients_mutex);
            (*client_data->topicsClientsList)[requestVector[1]].remove(client_data->client_descriptor);
            pthread_mutex_unlock(client_data->topics_clients_mutex);
        }

        else if (requestVector[0] == "SENDMESSAGE") {
            std::string message = "MESSAGE//divider@//" + requestString.substr(23, requestString.size()); // wiadomość do wysłania bez nagłówka 'SENDMESSAGE'

            int messageSize = message.size();
            if(messageSize > 1000)
                message = std::to_string(messageSize) + message;
            else if(messageSize > 100)
                message = "0" + std::to_string(messageSize) + message;
            else
                message = "00" + std::to_string(messageSize) + message;

            std::unique_ptr<char[]> messageCharTab(new char[message.size()]);
            strcpy(messageCharTab.get(), message.c_str());

            pthread_mutex_lock(client_data->topics_clients_mutex);
            if ((*client_data->topicsClientsList).find(requestVector[3]) != (*client_data->topicsClientsList).end() ) {
                for(int client : (*client_data->topicsClientsList).at(requestVector[3])) {
                    pthread_mutex_lock(&((*client_data->clients_mutex).at(client_data->client_descriptor)));
                    if(write(client, messageCharTab.get(), message.size()) < 0)
                        printf("Błąd podczas wysyłania wiadomości.");
                    pthread_mutex_unlock(&((*client_data->clients_mutex).at(client_data->client_descriptor)));
                }
            }
            pthread_mutex_unlock(client_data->topics_clients_mutex);
        }

        else if (requestVector[0] == "LOGOUT") {
            typedef std::map<std::string, std::list<int>>::const_iterator MapIterator;
            for (MapIterator iter = (*client_data->topicsClientsList).begin(); iter != (*client_data->topicsClientsList).end(); iter++) {
                std::list<int> clients = iter->second;
                for (int list_iter : iter->second) {
                    if (list_iter == client_data->client_descriptor) {
                        clients.remove(client_data->client_descriptor);
                        break;
                    }
                }
                (*client_data->topicsClientsList).at(iter->first) = clients;
            }
            break;
        }

	// wyświetlanie topicsClientsList
        printf("\n----------------- TOPICS and SUBSCRIBERS ----------------\n");
        typedef std::map<std::string, std::list<int>>::const_iterator MapIterator;
        for (MapIterator iter = (*client_data->topicsClientsList).begin(); iter != (*client_data->topicsClientsList).end(); iter++)
        {
            printf("Topic: %s, Subscribers: ",(iter->first).c_str());
            for (int list_iter : iter->second)
                printf("%d, ",list_iter);
            printf("\n");
        }
        printf("---------------------------------------------------------\n");
    }

    close(client_data->client_descriptor);
    delete client_data;
    client_data = nullptr;

    return nullptr;
}

int main() {
    std::map<std::string, std::list<int>> *topicsClientsList = new std::map<std::string, std::list<int>>;   // <-- Kluczami są nazwy tematów, wartościami listy
                                                                                                            //     subskrybujących dany temat użytkowników (konkretniej
                                                                                                            //     ich deskryptory)

    std::map<int, pthread_mutex_t> *clients_mutex = new std::map<int, pthread_mutex_t>;                     // <-- mapa przechowująca mutexy dla klientów,

    pthread_mutex_t	topics_clients_mutex = PTHREAD_MUTEX_INITIALIZER;;  // mutex na topicsClientsList
    pthread_mutex_t mutex_map = PTHREAD_MUTEX_INITIALIZER;              // mutex na mapę z mutexami klientów


    struct sockaddr_in server_addr;

    int server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if(server_socket < 0 ) {
        printf("Błąd przy próbie utworzenia gniazda.");
        delete topicsClientsList;
        exit(-1);
    }

    memset(&server_addr, 0 , sizeof(struct sockaddr_in));
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons(2317);

    int opt = 1;
    setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    if (bind(server_socket, (struct sockaddr *) &server_addr, sizeof(server_addr)) < 0) {
        printf("Błąd przy próbie dowiązania adresu IP i numeru portu do gniazda.");
        delete topicsClientsList;
        delete clients_mutex;
        exit(-1);
    }

    if (listen(server_socket, QUEUE_SIZE) < 0) {
        printf("Błąd przy próbie ustawienia wielkości kolejki.");
        delete topicsClientsList;
        delete clients_mutex;
        exit(-1);
    }

    while(true) {
        int client = accept(server_socket, NULL, NULL);
        if (client < 0) {
            printf("Błąd przy próbie utworzenia gniazda dla połączenia.");
            delete topicsClientsList;
            delete clients_mutex;
            exit(-1);
        }

        pthread_mutex_lock(&mutex_map);
        clients_mutex->insert(std::pair<int, pthread_mutex_t>(client, PTHREAD_MUTEX_INITIALIZER));
        pthread_mutex_unlock(&mutex_map);

        // uchwyt na wątek
        pthread_t thread_client;

        // dane, które zostaną przekazane do wątku
        ThreadData *t_data = new ThreadData;
        t_data->client_descriptor = client;
        t_data->topicsClientsList = topicsClientsList;
        t_data->clients_mutex = clients_mutex;
        t_data->topics_clients_mutex = &topics_clients_mutex;

        // stworzenie oddzielnego wątku, dla każdego klienta
        int create_result = 0;
        create_result = pthread_create(&thread_client, NULL, clientThread, (void *)t_data);
        if (create_result){
            printf("Błąd przy próbie utworzenia wątku thread_client, kod błędu: %d\n", create_result);
            delete topicsClientsList;
            delete clients_mutex;
            exit(-1);
        }
    }

    delete topicsClientsList;
    delete clients_mutex;

    close(server_socket);
}
