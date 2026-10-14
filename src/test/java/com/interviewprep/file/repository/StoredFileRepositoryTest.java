package com.interviewprep.file.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewprep.file.entity.StoredFile;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StoredFileRepositoryTest {

    @Autowired
    private StoredFileRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("FILE-01 saving a record generates its id and upload time")
    void saveGeneratesIdAndUploadTime() {
        StoredFile file = new StoredFile("photo.png", UUID.randomUUID().toString(), "image/png", 2048);

        StoredFile saved = repository.saveAndFlush(file);
        entityManager.clear();

        StoredFile reloaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getUploadedAt()).isNotNull();
        assertThat(reloaded.getOriginalName()).isEqualTo("photo.png");
        assertThat(reloaded.getStoredName()).isEqualTo(file.getStoredName());
        assertThat(reloaded.getContentType()).isEqualTo("image/png");
        assertThat(reloaded.getSize()).isEqualTo(2048);
    }

    @Test
    @DisplayName("FILE-06 records page newest first")
    void pagesNewestFirst() {
        save("oldest.pdf", Instant.parse("2026-01-01T00:00:00Z"));
        StoredFile newest = save("newest.pdf", Instant.parse("2026-03-01T00:00:00Z"));
        StoredFile middle = save("middle.pdf", Instant.parse("2026-02-01T00:00:00Z"));
        entityManager.clear();

        Page<StoredFile> page = repository.findAll(PageRequest.of(0, 2, Sort.by(Sort.Order.desc("uploadedAt"))));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(StoredFile::getId).containsExactly(newest.getId(), middle.getId());
    }

    @Test
    @DisplayName("FILE-01 two records cannot share a stored name")
    void storedNameIsUnique() {
        String storedName = UUID.randomUUID().toString();
        repository.saveAndFlush(new StoredFile("a.png", storedName, "image/png", 10));

        StoredFile duplicate = new StoredFile("b.png", storedName, "image/png", 10);

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private StoredFile save(String name, Instant uploadedAt) {
        StoredFile saved =
                repository.saveAndFlush(new StoredFile(name, UUID.randomUUID().toString(), "application/pdf", 100));
        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE stored_files SET uploaded_at = :uploadedAt WHERE id = :id")
                .setParameter("uploadedAt", uploadedAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        return saved;
    }
}
