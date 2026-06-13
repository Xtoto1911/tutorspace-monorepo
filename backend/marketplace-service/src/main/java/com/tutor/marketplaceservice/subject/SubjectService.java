package com.tutor.marketplaceservice.subject;

import com.tutor.marketplaceservice.shared.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public List<SubjectDto> listSubjects() {
        return subjectRepository.findAll().stream()
                .map(SubjectDto::from)
                .toList();
    }

    @Transactional
    public SubjectDto createSubject(UpsertSubjectRequest request) {
        if (subjectRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Subject slug already exists: " + request.slug());
        }

        Subject subject = new Subject();
        subject.setName(request.name());
        subject.setSlug(request.slug());
        subject.setDescription(request.description());

        return SubjectDto.from(subjectRepository.save(subject));
    }

    @Transactional(readOnly = true)
    public Subject getSubject(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));
    }

    @Transactional(readOnly = true)
    public Set<Subject> resolveSubjectsByNamesOrDefault(List<String> names) {
        Set<Subject> resolved = new LinkedHashSet<>();
        if (names != null) {
            names.stream()
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .map(subjectRepository::findByNameIgnoreCase)
                    .flatMap(Optional::stream)
                    .forEach(resolved::add);
        }

        if (resolved.isEmpty()) {
            subjectRepository.findAll().stream().findFirst().ifPresent(resolved::add);
        }

        return resolved;
    }
}
