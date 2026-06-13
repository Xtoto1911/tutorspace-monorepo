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

            createReview(reviewRepository, anna, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1"), 5,
                    "За два месяца дочь стала спокойнее решать пробники и перестала теряться на сложных задачах.");
            createReview(reviewRepository, anna, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"), 5,
                    "Очень понятная подача, много практики и быстрый разбор ошибок.");
            createReview(reviewRepository, pavel, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3"), 5,
                    "Стало понятно, как решать задачи на алгоритмы и писать код без паники.");
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
