package com.tutor.marketplaceservice.subject;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/marketplace/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public List<SubjectDto> listSubjects() {
        return subjectService.listSubjects();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectDto createSubject(@Valid @RequestBody UpsertSubjectRequest request) {
        return subjectService.createSubject(request);
    }
}
