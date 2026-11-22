package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.book.BookStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class VocabularyTest {

    private final Vocabulary vocab = new Vocabulary();

    @Test
    void fuzzyContainsMatchesExactPhrase() {
        assertThat(vocab.fuzzyContains("how many books do i have", "how many")).isTrue();
    }

    @Test
    void fuzzyContainsToleratesOneTypoPerWord() {
        assertThat(vocab.fuzzyContains("show my fantsy books", "fantasy")).isTrue();
        assertThat(vocab.fuzzyContains("show the most expesnive book", "expensive")).isTrue();
    }

    @Test
    void fuzzyContainsRejectsUnrelatedText() {
        assertThat(vocab.fuzzyContains("what is the weather today", "expensive")).isFalse();
    }

    @Test
    void fuzzyContainsDoesNotFalsePositiveOnCommonShortWords() {
        assertThat(vocab.fuzzyContains("who owns the most books", "cost")).isFalse();
    }

    @Test
    void matchStatusRecognizesEnglishAndAlbanian() {
        assertThat(vocab.matchStatus("how many books are unread?")).isEqualTo(BookStatus.TO_READ);
        assertThat(vocab.matchStatus("sa libra kam pa lexuar?")).isEqualTo(BookStatus.TO_READ);
        assertThat(vocab.matchStatus("books i am currently reading")).isEqualTo(BookStatus.READING);
        assertThat(vocab.matchStatus("books i have completed")).isEqualTo(BookStatus.COMPLETED);
        assertThat(vocab.matchStatus("show me all my books")).isNull();
    }

    @Test
    void matchExplicitFieldPrefersPagesOverPrice() {
        assertThat(vocab.matchExplicitField("average pages of my books")).isEqualTo(Field.PAGES);
        assertThat(vocab.matchExplicitField("average price of my books")).isEqualTo(Field.PRICE);
        assertThat(vocab.matchExplicitField("books by author")).isEqualTo(Field.AUTHOR);
    }

    @Test
    void matchNumberReadsDigitsAndWordsInBothLanguages() {
        assertThat(vocab.matchNumber("show the top 3 books")).isEqualTo(3);
        assertThat(vocab.matchNumber("show the top five books")).isEqualTo(5);
        assertThat(vocab.matchNumber("trego 2 libra")).isEqualTo(2);
        assertThat(vocab.matchNumber("show the top twenty books")).isEqualTo(20);
    }

    @Test
    void findKnownValueMatchesExactAndTypoedGenre() {
        List<String> genres = List.of("fantasy", "sci-fi");
        assertThat(vocab.findKnownValue("show my fantasy books", genres)).isEqualTo("fantasy");
        assertThat(vocab.findKnownValue("show my fantsy books", genres)).isEqualTo("fantasy");
        assertThat(vocab.findKnownValue("show my sci-fi books", genres)).isEqualTo("sci-fi");
        assertThat(vocab.findKnownValue("show my horror books", genres)).isNull();
    }

    @Test
    void findKnownValueMatchesMultiWordAuthor() {
        List<String> authors = List.of("frank herbert", "j.r.r. tolkien");
        assertThat(vocab.findKnownValue("books by frank herbert", authors)).isEqualTo("frank herbert");
        assertThat(vocab.findKnownValue("longest book by tolkien", List.of("tolkien"))).isEqualTo("tolkien");
    }
}
