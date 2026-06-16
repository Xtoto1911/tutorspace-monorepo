package com.tutor.marketplaceservice.config;

import com.tutor.marketplaceservice.review.Review;
import com.tutor.marketplaceservice.review.ReviewRepository;
import com.tutor.marketplaceservice.review.ReviewStatus;
import com.tutor.marketplaceservice.subject.Subject;
import com.tutor.marketplaceservice.subject.SubjectRepository;
import com.tutor.marketplaceservice.tutor.TutorProfile;
import com.tutor.marketplaceservice.tutor.TutorProfileRepository;
import com.tutor.marketplaceservice.tutor.TutorProfileStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
public class MarketplaceSeedConfig {

    @Bean
    CommandLineRunner marketplaceSeedRunner(SubjectRepository subjectRepository,
                                            TutorProfileRepository tutorProfileRepository,
                                            ReviewRepository reviewRepository) {
        return args -> {
            if (subjectRepository.count() > 0 || tutorProfileRepository.count() > 0) {
                return;
            }

            Subject math = createSubject(subjectRepository, "Математика", "matematika",
                    "Школьная математика, алгебра, геометрия и подготовка к ОГЭ, ЕГЭ и олимпиадам.");
            Subject english = createSubject(subjectRepository, "Английский", "angliiskii-yazyk",
                    "Разговорный и академический английский, школьная программа и подготовка к экзаменам.");
            Subject it = createSubject(subjectRepository, "Информатика", "informatika",
                    "Программирование, алгоритмы, компьютерная грамотность и подготовка к экзаменам.");
            Subject russian = createSubject(subjectRepository, "Русский", "russkii-yazyk",
                    "Грамматика, сочинение, повышение грамотности и подготовка к ОГЭ и ЕГЭ.");
            Subject physics = createSubject(subjectRepository, "Физика", "fizika",
                    "Школьная физика, механика, термодинамика, электричество и подготовка к ЕГЭ и олимпиадам.");
            Subject chemistry = createSubject(subjectRepository, "Химия", "himiya",
                    "Общая и органическая химия, подготовка к ОГЭ и ЕГЭ, олимпиадные задачи.");
            Subject biology = createSubject(subjectRepository, "Биология", "biologiya",
                    "Школьная биология, ботаника, зоология, анатомия и подготовка к экзаменам.");
            Subject history = createSubject(subjectRepository, "История", "istoriya",
                    "История России и мира, подготовка к ОГЭ и ЕГЭ, олимпиадные задачи.");

            TutorProfile anna = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    "anna-petrova",
                    "Анна Смирнова",
                    "Помогаю школьникам уверенно подготовиться к ОГЭ и ЕГЭ по математике.",
                    "Москва",
                    List.of("МГУ, механико-математический факультет"),
                    List.of("Средний результат учеников на ЕГЭ — 84 балла", "Более 120 учеников завершили курс"),
                    List.of("ОГЭ", "ЕГЭ", "Алгебра"),
                    20,
                    7,
                    "https://example.com/anna.jpg",
                    new BigDecimal("1800"),
                    4.9,
                    128,
                    Set.of(math)
            );
            TutorProfile ivan = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    "ivan-smirnov",
                    "Иван Смирнов",
                    "Системная подготовка к математике и олимпиадным задачам.",
                    "Санкт-Петербург",
                    List.of("СПбГУ, математико-механический факультет"),
                    List.of("Призеры региональных олимпиад среди учеников"),
                    List.of("Олимпиады", "Геометрия"),
                    45,
                    6,
                    "https://example.com/ivan.jpg",
                    new BigDecimal("1200"),
                    4.8,
                    98,
                    Set.of(math)
            );
            TutorProfile maria = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("33333333-3333-3333-3333-333333333333"),
                    "mariya-ivanova",
                    "Мария Иванова",
                    "Разговорная практика и подготовка к экзаменам по английскому.",
                    "Казань",
                    List.of("КФУ, Институт международных отношений", "Сертификат CELTA"),
                    List.of("Занятия полностью на английском с уровня B1"),
                    List.of("Разговорный английский", "ЕГЭ", "IELTS"),
                    15,
                    7,
                    "https://example.com/maria.jpg",
                    new BigDecimal("1700"),
                    4.9,
                    112,
                    Set.of(english)
            );
            TutorProfile pavel = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("44444444-4444-4444-4444-444444444444"),
                    "pavel-egorov",
                    "Павел Егоров",
                    "Обучаю Python и алгоритмам через практические задачи и мини-проекты.",
                    "Новосибирск",
                    List.of("НГУ, факультет информационных технологий"),
                    List.of("Каждый ученик собирает портфолио из учебных проектов"),
                    List.of("Python", "Алгоритмы", "Проекты"),
                    60,
                    10,
                    "https://example.com/pavel.jpg",
                    new BigDecimal("2400"),
                    4.7,
                    87,
                    Set.of(it)
            );
            TutorProfile irina = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("55555555-5555-5555-5555-555555555555"),
                    "irina-vorontsova",
                    "Ирина Воронцова",
                    "Русский язык без перегруза: понятные правила и стабильный прогресс.",
                    "Екатеринбург",
                    List.of("УрФУ, филологический факультет"),
                    List.of("Эксперт региональной предметной комиссии", "Средний результат ЕГЭ — 91 балл"),
                    List.of("ЕГЭ", "Сочинение", "Грамотность"),
                    30,
                    15,
                    "https://example.com/irina.jpg",
                    new BigDecimal("2600"),
                    5.0,
                    203,
                    Set.of(russian)
            );
            TutorProfile olga = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("66666666-6666-6666-6666-666666666666"),
                    "olga-sokolova",
                    "Ольга Соколова",
                    "Готовлю к ЕГЭ по физике: разбираю сложные темы через жизненные примеры и эксперименты.",
                    "Москва",
                    List.of("МФТИ, факультет общей и прикладной физики"),
                    List.of("Средний балл учеников на ЕГЭ — 86"),
                    List.of("ЕГЭ", "Механика", "Олимпиады"),
                    25,
                    9,
                    "https://example.com/olga.jpg",
                    new BigDecimal("2000"),
                    4.8,
                    76,
                    Set.of(physics)
            );
            TutorProfile sergey = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("77777777-7777-7777-7777-777777777777"),
                    "sergey-belov",
                    "Сергей Белов",
                    "Объясняю информатику простым языком: от переменных до олимпиадных алгоритмов.",
                    "Санкт-Петербург",
                    List.of("ИТМО, факультет информационных технологий и программирования"),
                    List.of("Победитель всероссийской олимпиады по информатике 2018"),
                    List.of("Python", "C++", "Алгоритмы"),
                    40,
                    8,
                    "https://example.com/sergey.jpg",
                    new BigDecimal("2200"),
                    4.7,
                    64,
                    Set.of(it)
            );
            TutorProfile elena = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("88888888-8888-8888-8888-888888888888"),
                    "elena-zaytseva",
                    "Елена Зайцева",
                    "Помогаю освоить английский с нуля и подготовиться к международным экзаменам.",
                    "Нижний Новгород",
                    List.of("ННГУ, факультет иностранных языков", "Сертификат TEFL"),
                    List.of("Индивидуальная программа под цели и уровень ученика"),
                    List.of("Для начинающих", "IELTS", "Speaking"),
                    20,
                    6,
                    "https://example.com/elena.jpg",
                    new BigDecimal("1400"),
                    4.6,
                    54,
                    Set.of(english)
            );
            TutorProfile dmitry = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("99999999-9999-9999-9999-999999999999"),
                    "dmitry-frolov",
                    "Дмитрий Фролов",
                    "Химия без зубрёжки: логика реакций, задачи на равновесие и олимпиадные приёмы.",
                    "Краснодар",
                    List.of("КубГУ, химический факультет"),
                    List.of("Призёр олимпиады «Ломоносов» по химии"),
                    List.of("ЕГЭ", "Олимпиады", "Органика"),
                    35,
                    11,
                    "https://example.com/dmitry.jpg",
                    new BigDecimal("1900"),
                    4.9,
                    91,
                    Set.of(chemistry)
            );
            TutorProfile natalia = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                    "natalia-kim",
                    "Наталья Ким",
                    "Биология и генетика понятным языком: от школы до олимпиад.",
                    "Самара",
                    List.of("СамГУ, биологический факультет"),
                    List.of("Готовлю к олимпиадам «Покори Воробьёвы горы» и «Ломоносов»"),
                    List.of("ЕГЭ", "Генетика", "Олимпиады"),
                    30,
                    7,
                    "https://example.com/natalia.jpg",
                    new BigDecimal("1500"),
                    4.5,
                    42,
                    Set.of(biology)
            );
            TutorProfile andrey = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01"),
                    "andrey-tarasov",
                    "Андрей Тарасов",
                    "История России: от Рюрика до перестройки в коротких и ярких сюжетах.",
                    "Уфа",
                    List.of("БашГУ, исторический факультет"),
                    List.of("Учу работать с источниками и аргументировать позицию"),
                    List.of("ЕГЭ", "Олимпиады", "История России"),
                    30,
                    12,
                    "https://example.com/andrey.jpg",
                    new BigDecimal("1300"),
                    4.4,
                    38,
                    Set.of(history)
            );
            TutorProfile ksenia = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02"),
                    "ksenia-golubeva",
                    "Ксения Голубева",
                    "Математика для 5–9 классов: прочный фундамент и уверенность на контрольных.",
                    "Воронеж",
                    List.of("ВГУ, математический факультет"),
                    List.of("Подготовила 30+ призёров муниципальных олимпиад"),
                    List.of("5–9 класс", "ОГЭ", "Алгебра"),
                    15,
                    4,
                    "https://example.com/ksenia.jpg",
                    new BigDecimal("1100"),
                    4.7,
                    49,
                    Set.of(math)
            );
            TutorProfile mikhail = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03"),
                    "mikhail-vorobyov",
                    "Михаил Воробьёв",
                    "Английский для IT: технический английский, собеседования, чтение документации.",
                    "Москва",
                    List.of("МГТУ им. Баумана, лингвистика", "Опыт работы в IT-компаниях 8 лет"),
                    List.of("Помогаю готовиться к интервью в зарубежные команды"),
                    List.of("IT English", "Speaking", "Tech Docs"),
                    20,
                    9,
                    "https://example.com/mikhail.jpg",
                    new BigDecimal("2800"),
                    4.9,
                    110,
                    Set.of(english)
            );
            TutorProfile tatiana = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb04"),
                    "tatiana-ignatieva",
                    "Татьяна Игнатьева",
                    "Русский для иностранцев и школьников: грамматика, разговорная речь, сочинение.",
                    "Санкт-Петербург",
                    List.of("РГПУ им. Герцена, филологический факультет"),
                    List.of("Сертификат РКИ, опыт работы с билингвами 10 лет"),
                    List.of("РКИ", "Грамматика", "Сочинение"),
                    20,
                    14,
                    "https://example.com/tatiana.jpg",
                    new BigDecimal("1600"),
                    4.7,
                    68,
                    Set.of(russian)
            );
            TutorProfile alexey = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb05"),
                    "alexey-grigoriev",
                    "Алексей Григорьев",
                    "Информатика и олимпиадное программирование: Pascal, C++, Python.",
                    "Казань",
                    List.of("КФУ, институт вычислительной математики и информационных технологий"),
                    List.of("Тренер региональной сборной по информатике"),
                    List.of("Олимпиады", "C++", "Python"),
                    60,
                    13,
                    "https://example.com/alexey.jpg",
                    new BigDecimal("3000"),
                    4.8,
                    73,
                    Set.of(it)
            );
            TutorProfile vera = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb06"),
                    "vera-alekseeva",
                    "Вера Алексеева",
                    "Химия для 8–11 классов: понятные объяснения и большой банк задач.",
                    "Екатеринбург",
                    List.of("УрФУ, институт естественных наук и математики"),
                    List.of("Все ученики за последние 3 года сдали ОГЭ на «4» и «5»"),
                    List.of("ОГЭ", "ЕГЭ", "8–9 класс"),
                    15,
                    5,
                    "https://example.com/vera.jpg",
                    new BigDecimal("1200"),
                    4.6,
                    41,
                    Set.of(chemistry)
            );
            TutorProfile boris = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb07"),
                    "boris-mironov",
                    "Борис Миронов",
                    "Физика и математика для олимпиад: разбор задач МФТИ, «Физтех», Всеросса.",
                    "Москва",
                    List.of("МФТИ, ФОПФ"),
                    List.of("Два призёра Всероссийской олимпиады школьников"),
                    List.of("Олимпиады", "Физика", "Математика"),
                    90,
                    18,
                    "https://example.com/boris.jpg",
                    new BigDecimal("3500"),
                    5.0,
                    156,
                    Set.of(physics, math)
            );
            TutorProfile polina = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb08"),
                    "polina-orlova",
                    "Полина Орлова",
                    "Английский для малышей 4–7 лет через игры, песни и сказки.",
                    "Новосибирск",
                    List.of("НГПУ, факультет иностранных языков"),
                    List.of("Опыт работы в детских языковых центрах 6 лет"),
                    List.of("Для малышей", "Игры", "Speaking"),
                    10,
                    3,
                    "https://example.com/polina.jpg",
                    new BigDecimal("900"),
                    4.8,
                    32,
                    Set.of(english)
            );
            TutorProfile victor = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb09"),
                    "victor-antonov",
                    "Виктор Антонов",
                    "Подготовка к олимпиадам по истории: работа с источниками, картами, датами.",
                    "Санкт-Петербург",
                    List.of("ЕУ СПб, кафедра истории"),
                    List.of("Член жюри регионального этапа Всеросса по истории"),
                    List.of("Олимпиады", "История России", "Всемирная история"),
                    45,
                    16,
                    "https://example.com/victor.jpg",
                    new BigDecimal("1700"),
                    4.7,
                    58,
                    Set.of(history)
            );
            TutorProfile lidia = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0a"),
                    "lidia-stepanova",
                    "Лидия Степанова",
                    "Биология и анатомия для поступающих в медицинские вузы.",
                    "Москва",
                    List.of("Первый МГМУ им. Сеченова"),
                    List.of("Подготовила 50+ студентов к поступлению в медвузы"),
                    List.of("ЕГЭ", "Анатомия", "Поступление в мед"),
                    20,
                    10,
                    "https://example.com/lidia.jpg",
                    new BigDecimal("2500"),
                    4.9,
                    89,
                    Set.of(biology)
            );
            TutorProfile artem = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0b"),
                    "artem-kuzmin",
                    "Артём Кузьмин",
                    "Информатика и математика на русском и английском: C++, Java, Python.",
                    "Москва",
                    List.of("ВШЭ, факультет компьютерных наук"),
                    List.of("Software engineer в международной компании"),
                    List.of("Java", "C++", "Олимпиады"),
                    30,
                    6,
                    "https://example.com/artem.jpg",
                    new BigDecimal("2300"),
                    4.6,
                    47,
                    Set.of(it, math)
            );
            TutorProfile svetlana = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0c"),
                    "svetlana-moroz",
                    "Светлана Мороз",
                    "Математика и физика для учеников 7–11 классов: спокойная подача и опора на практику.",
                    "Нижний Новгород",
                    List.of("ННГУ, радиофизический факультет"),
                    List.of("Средний балл учеников на ОГЭ — 4.7"),
                    List.of("7–11 класс", "ОГЭ", "ЕГЭ"),
                    20,
                    8,
                    "https://example.com/svetlana.jpg",
                    new BigDecimal("1500"),
                    4.7,
                    55,
                    Set.of(math, physics)
            );
            TutorProfile kirill = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0d"),
                    "kirill-romanov",
                    "Кирилл Романов",
                    "Химия и биология в комплексе: готовлю к олимпиадам и поступлению в вуз.",
                    "Воронеж",
                    List.of("ВГУ, медико-биологический факультет"),
                    List.of("Победитель олимпиады «Шаг в будущее» по химии 2017"),
                    List.of("Химия", "Биология", "Олимпиады"),
                    30,
                    5,
                    "https://example.com/kirill.jpg",
                    new BigDecimal("1800"),
                    4.8,
                    39,
                    Set.of(chemistry, biology)
            );
            TutorProfile julia = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0e"),
                    "julia-belyaeva",
                    "Юлия Беляева",
                    "Английский и русский для школьников: домашние задания, контрольные, ЕГЭ.",
                    "Краснодар",
                    List.of("КубГУ, факультет романо-германской филологии"),
                    List.of("Подготовила 40+ учеников к ОГЭ и ЕГЭ на «отлично»"),
                    List.of("Английский", "Русский", "ЕГЭ"),
                    15,
                    4,
                    "https://example.com/julia.jpg",
                    new BigDecimal("1300"),
                    4.7,
                    44,
                    Set.of(english, russian)
            );
            TutorProfile maxim = createTutor(
                    tutorProfileRepository,
                    UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb0f"),
                    "maxim-kovalev",
                    "Максим Ковалёв",
                    "История и обществознание: подготовка к ЕГЭ и олимпиадам МГУ, ВШЭ.",
                    "Самара",
                    List.of("СамГУ, исторический факультет"),
                    List.of("Два победителя олимпиады «Высшая проба» по истории"),
                    List.of("История", "Обществознание", "Олимпиады"),
                    30,
                    9,
                    "https://example.com/maxim.jpg",
                    new BigDecimal("1700"),
                    4.8,
                    52,
                    Set.of(history)
            );

            createReview(reviewRepository, anna, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1"), 5,
                    "За два месяца дочь стала спокойнее решать пробники и перестала теряться на сложных задачах.");
            createReview(reviewRepository, anna, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"), 5,
                    "Очень понятная подача, много практики и быстрый разбор ошибок.");
            createReview(reviewRepository, pavel, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3"), 5,
                    "Стало понятно, как решать задачи на алгоритмы и писать код без паники.");
            createReview(reviewRepository, ivan, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4"), 5,
                    "Олимпиадные задачи перестали пугать: разбираем по шагам, без воды.");
            createReview(reviewRepository, maria, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5"), 4,
                    "Хороший преподаватель, дочь стала увереннее говорить на английском.");
            createReview(reviewRepository, irina, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6"), 5,
                    "Сочинение перестало быть стрессом: чёткая структура и понятные критерии.");
            createReview(reviewRepository, olga, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7"), 5,
                    "Наконец-то понял термодинамику, благодаря экспериментам и примерам из жизни.");
            createReview(reviewRepository, sergey, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa8"), 4,
                    "Сын стал увереннее решать задачи по алгоритмам, прогресс заметен.");
            createReview(reviewRepository, elena, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa9"), 5,
                    "Занимаемся всего месяц, а разговорная речь уже намного живее.");
            createReview(reviewRepository, dmitry, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa10"), 5,
                    "Химия стала понятной: логика реакций вместо зубрёжки.");
            createReview(reviewRepository, natalia, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa11"), 4,
                    "Дочь готовится к олимпиаде, материал подаётся системно.");
            createReview(reviewRepository, andrey, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa12"), 5,
                    "Исторические события теперь связаны в единую картину, запоминать легче.");
            createReview(reviewRepository, ksenia, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa13"), 5,
                    "Очень внимательный преподаватель, ребёнок перестал бояться контрольных.");
            createReview(reviewRepository, mikhail, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa14"), 5,
                    "Помог подготовиться к техническому интервью на английском, успешно прошёл.");
            createReview(reviewRepository, tatiana, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa15"), 4,
                    "Спокойная и грамотная подача, подходит для нашего билингва.");
            createReview(reviewRepository, alexey, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa16"), 5,
                    "Реально подтянул сына с нуля до уровня олимпиад за полгода.");
            createReview(reviewRepository, vera, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa17"), 4,
                    "Хорошо объясняет, ребёнок с нетерпением ждёт занятий.");
            createReview(reviewRepository, boris, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa18"), 5,
                    "Олимпиадный подход, задачи разбираются на уровне, который нигде больше не дают.");
            createReview(reviewRepository, polina, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa19"), 5,
                    "Дочь 5 лет бежит на занятия, уже знает 30 слов и песенку.");
            createReview(reviewRepository, victor, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa20"), 4,
                    "Сильно помог с подготовкой к Всероссу, структурировал материал.");
            createReview(reviewRepository, lidia, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa21"), 5,
                    "Поступила в медвуз благодаря занятиям, очень благодарна преподавателю.");
            createReview(reviewRepository, artem, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa22"), 4,
                    "Хороший специалист, объясняет сложные темы простым языком.");
            createReview(reviewRepository, svetlana, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa23"), 5,
                    "Наконец-то ребёнок стал понимать физику, а не зубрить формулы.");
            createReview(reviewRepository, kirill, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa24"), 5,
                    "Химия и биология в комплексе очень помогли на олимпиаде.");
            createReview(reviewRepository, julia, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa25"), 4,
                    "Спокойная подача, ребёнок не устаёт, прогресс есть.");
        };
    }

    private Subject createSubject(SubjectRepository repository, String name, String slug, String description) {
        Subject subject = new Subject();
        subject.setName(name);
        subject.setSlug(slug);
        subject.setDescription(description);
        return repository.save(subject);
    }

    private TutorProfile createTutor(TutorProfileRepository repository,
                                     UUID userId,
                                     String slug,
                                     String fullName,
                                     String description,
                                     String city,
                                     List<String> education,
                                     List<String> highlights,
                                     List<String> tags,
                                     Integer responseTimeMinutes,
                                     int experienceYears,
                                     String photoUrl,
                                     BigDecimal pricePerHour,
                                     double rating,
                                     int reviewsCount,
                                     Set<Subject> subjects) {
        TutorProfile profile = new TutorProfile();
        profile.setUserId(userId);
        profile.setSlug(slug);
        profile.setFullName(fullName);
        profile.setDescription(description);
        profile.setCity(city);
        profile.setEducation(education);
        profile.setHighlights(highlights);
        profile.setTags(tags);
        profile.setResponseTimeMinutes(responseTimeMinutes);
        profile.setExperienceYears(experienceYears);
        profile.setPhotoUrl(photoUrl);
        profile.setPricePerHour(pricePerHour);
        profile.setRating(rating);
        profile.setReviewsCount(reviewsCount);
        profile.setStatus(TutorProfileStatus.ACTIVE);
        profile.setSubjects(new LinkedHashSet<>(subjects));
        return repository.save(profile);
    }

    private void createReview(ReviewRepository repository,
                              TutorProfile tutorProfile,
                              UUID clientUserId,
                              int rating,
                              String text) {
        Review review = new Review();
        review.setTutorProfile(tutorProfile);
        review.setClientUserId(clientUserId);
        review.setRating(rating);
        review.setText(text);
        review.setStatus(ReviewStatus.PUBLISHED);
        repository.save(review);
    }
}
