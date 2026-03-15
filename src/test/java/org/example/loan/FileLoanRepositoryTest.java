package org.example.loan;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.lending.MemberId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileLoanRepositoryTest extends LoanRepositoryContractTest {

    @TempDir
    Path tempDir;

    @Override
    protected LoanRepository createRepository() {
        return new FileLoanRepository(tempDir.resolve("loans.json"), new ObjectMapper());
    }

    @Test
    void shouldThrowPersistenceErrorOnCorruptData() throws IOException {
        Files.writeString(tempDir.resolve("loans.json"), "corrupt data");
        assertThatThrownBy(() -> repository.findBooksByMember(new MemberId("123")))
                .isInstanceOf(LoanPersistenceException.class);
    }
}
