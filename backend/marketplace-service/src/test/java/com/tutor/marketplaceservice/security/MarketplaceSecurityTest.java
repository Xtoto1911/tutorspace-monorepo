package com.tutor.marketplaceservice.security;

import com.tutor.marketplaceservice.application.TutorApplicationService;
import com.tutor.marketplaceservice.review.ReviewRepository;
import com.tutor.marketplaceservice.review.ReviewService;
import com.tutor.marketplaceservice.subject.SubjectService;
import com.tutor.marketplaceservice.tutor.TutorProfileRepository;
import com.tutor.marketplaceservice.tutor.TutorProfileService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(properties = "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class, JwtProperties.class})
class MarketplaceSecurityTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TutorProfileService tutorProfileService;

    @MockitoBean
    private TutorProfileRepository tutorProfileRepository;

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private TutorApplicationService tutorApplicationService;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @Test
    void publicCatalogReadDoesNotRequireToken() throws Exception {
        when(tutorProfileService.search(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/marketplace/tutors"))
                .andExpect(status().isOk());
    }

    @Test
    void writeEndpointRequiresToken() throws Exception {
        mockMvc.perform(post("/api/marketplace/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewEligibilityRequiresClientToken() throws Exception {
        mockMvc.perform(get("/api/marketplace/reviews/eligibility")
                        .param("tutorProfileId", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientTokenCanCreateReview() throws Exception {
        UUID tokenUserId = UUID.randomUUID();
        String token = token("ROLE_CLIENT", tokenUserId);

        mockMvc.perform(post("/api/marketplace/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tutorProfileId": "11111111-1111-1111-1111-111111111111",
                                  "clientUserId": "22222222-2222-2222-2222-222222222222",
                                  "rating": 5,
                                  "text": "Great tutor"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(reviewService).create(any(), eq(tokenUserId));
    }

    @Test
    void tutorTokenCannotCreateReview() throws Exception {
        String token = token("ROLE_TUTOR");

        mockMvc.perform(post("/api/marketplace/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tutorProfileId": "11111111-1111-1111-1111-111111111111",
                                  "clientUserId": "22222222-2222-2222-2222-222222222222",
                                  "rating": 5,
                                  "text": "Great tutor"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void tutorTokenCanReportReviewComplaint() throws Exception {
        UUID tokenUserId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        String token = token("ROLE_TUTOR", tokenUserId);

        mockMvc.perform(post("/api/marketplace/reviews/{reviewId}/complaints", reviewId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Некорректный отзыв",
                                  "description": "Похоже на спам"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(reviewService).createComplaint(eq(reviewId), eq(tokenUserId), any());
    }

    private String token(String role) {
        return token(role, UUID.randomUUID());
    }

    private String token(String role, UUID userId) {
        return Jwts.builder()
                .claims(Map.of(
                        "userId", userId.toString(),
                        "role", role
                ))
                .subject("user@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
}
