# Client Schedule Design

**Goal**

Довести `/app/client/schedule` до полноценного live-экрана клиента: загрузка реальных данных из `crm-service`, корректный UI без декоративных заглушек, рабочие действия страницы и минимальный сценарий запроса переноса занятия.

**Current State**

- Маршрут уже подключен в `frontend/src/App.tsx` и защищен ролью `parent`.
- Страница `frontend/src/pages/app/ClientSchedulePage.tsx` рендерит визуально богатый экран, но фактически держится на локальном `fallbackData` и на page DTO с ограниченным смыслом.
- `crm-service` уже отдает `GET /api/client/schedule` через `ClientCrmController#getSchedule`, но `PageViewService#getClientSchedulePage` заполняет часть полей статикой: фиксированные `timeSlots`, фиктивная домашка, искусственный статус, искусственные переносы.
- Текущий DTO `ClientSchedulePageDto` не несет связей, нужных для рабочих CTA: нет `lessonId`, `applicationId`, `threadId`, признаков join/reschedule, пустых состояний и статуса синхронизации.

**Design Choice**

Используется один page-oriented live endpoint для страницы расписания вместо набора разрозненных запросов.

Почему:

- экран уже задуман как агрегатный;
- данные собираются внутри `PageViewService`, это естественная точка расширения;
- так можно одновременно решить live data, UI-state и action wiring без второго слоя клиентской склейки.

**Frontend Changes**

Файл: `frontend/src/pages/app/ClientSchedulePage.tsx`

- Убрать дублирующий основной `fallbackData` сценарий. Fallback оставить только как аварийное поведение при ошибке загрузки, но основной путь должен опираться на live DTO.
- Добавить явные состояния загрузки и ошибки.
- Перестроить рендер так, чтобы он честно отображал:
  - пустое расписание;
  - отсутствие переносов;
  - отсутствие ближайшего урока;
  - отсутствие домашних задач, если backend их пока не считает.
- CTA должны стать data-driven:
  - `Открыть урок` ведет в детали заявки, если есть `applicationId`, иначе в сообщения, если есть `threadId`;
  - `Написать репетитору` использует `threadId` и ведет в `/app/client/messages`;
  - `Запросить перенос` открывает минимальный action flow для выбранного занятия;
  - `Синхронизировать` просто перезагружает page DTO.
- Убрать из UI заведомо фейковые элементы вроде декоративного события `Окно / своб.` и тексты, которые не подтверждаются данными.

Файлы: `frontend/src/shared/api/client.ts`, `frontend/src/shared/types.ts`

- Расширить `ClientSchedulePageData` под реальное использование экрана.
- Обновить `getClientSchedulePage()` и добавить минимальный write helper для переноса занятия.

Новый shape page DTO на frontend должен включать, помимо текущих визуальных блоков:

- идентификаторы для CTA (`lessonId`, `applicationId`, `threadId` там, где релевантно);
- флаги доступности действий (`canReschedule`, `canJoin`, `canMessageTutor`);
- текстовые пустые состояния;
- источник/статус данных для синхронизации.

**Backend Changes**

Файлы: `backend/crm-service/src/main/java/com/tutor/crmservice/controller/ClientCrmController.java`, `backend/crm-service/src/main/java/com/tutor/crmservice/view/PageViewService.java`, `backend/crm-service/src/main/java/com/tutor/crmservice/view/ClientSchedulePageDto.java`

- Сохранить `GET /api/client/schedule` как основной read endpoint, но расширить его DTO.
- Убрать искусственные значения из `PageViewService#getClientSchedulePage` там, где уже можно вычислить данные из уроков, заявок и тредов.
- Для каждого ближайшего/календарного урока отдать достаточно данных для действий пользователя:
  - идентификатор урока;
  - идентификатор заявки, если урок вырос из нее;
  - идентификатор message thread, если он существует;
  - формат занятия;
  - время старта;
  - человекочитаемый статус;
  - разрешен ли перенос.
- Добавить минимальный write endpoint для запроса переноса. Предпочтительно не создавать полноценный новый workflow-модуль, а начать с простого CRM-действия уровня страницы.

Предлагаемый минимальный endpoint:

- `POST /api/client/schedule/lessons/{lessonId}/reschedule-request`

Минимальный payload:

- `message`: причина/комментарий клиента;
- `proposedStartAt`: опционально, если клиент предлагает конкретное время.

Минимальный результат:

- обновленный статус переноса для соответствующего урока;
- достаточно данных, чтобы frontend либо обновил страницу локально, либо сделал refetch.

**Interaction Model**

1. Пользователь открывает `/app/client/schedule`.
2. Frontend вызывает `GET /api/client/schedule`.
3. Backend возвращает агрегированный page DTO.
4. Frontend строит:
   - метрики;
   - неделю/календарь;
   - ближайший урок;
   - список переносов;
   - нижние карточки.
5. При `Синхронизировать` вызывается повторный `GET /api/client/schedule`.
6. При `Запросить перенос` frontend отправляет `POST /api/client/schedule/lessons/{lessonId}/reschedule-request`, затем обновляет экран.

**UI Rules**

- Страница должна выглядеть как клиентский календарь, а не как демо-панель.
- Реальные данные важнее декоративной плотности. Если данных мало, экран должен быть аккуратным, а не заполненным фейковыми карточками.
- Все CTA должны либо работать, либо быть скрыты/disabled на основе backend флагов. Никаких “живых” кнопок без действия.
- Русские тексты должны соответствовать состоянию пользователя: “уроков пока нет”, “запросов на перенос нет”, “репетитор еще не создал урок”.

**Non-Goals**

- Не вводить новый `calendar-service` или отдельный scheduler context.
- Не строить полноценный real-time sync или websocket-подписку.
- Не перерабатывать всю модель уроков в CRM, если для страницы хватает точечного расширения DTO и одного write action.

**Verification Plan**

Frontend:

- `npm test` в `frontend`
- `npm run build` в `frontend`

Backend:

- `mvn test` в `backend/crm-service`

Browser:

- `docker compose up --build`
- открыть `/app/client/schedule`
- проверить через devtools MCP:
  - загрузку live данных;
  - отображение пустых/непустых состояний;
  - `Синхронизировать`;
  - рабочий переход из CTA;
  - создание запроса на перенос и обновление UI.

**Risks**

- Локальный frontend proxy сейчас грубо шлет все `/api/tutor/**` в CRM и не имеет gateway. Для `client/schedule` это не мешает, но важно не проектировать решение в расчете на уже существующий gateway.
- В `crm-service` часть page data собирается на read-model уровне. Если нужные связи между уроком, заявкой и диалогом не хранятся явно, может понадобиться небольшой дополнительный mapping внутри CRM.
- Если live данных в сиде мало, нужно проверить, что UI достойно выглядит на почти пустом аккаунте.
