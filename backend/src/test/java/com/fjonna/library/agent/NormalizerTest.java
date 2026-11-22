package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NormalizerTest {

    @Test
    void lowercasesAndFoldsAlbanianDiacritics() {
        assertThat(Normalizer.normalize("Shtrenjtë Çmim")).isEqualTo("shtrenjte cmim");
    }

    @Test
    void handlesNull() {
        assertThat(Normalizer.normalize(null)).isEmpty();
    }

    @Test
    void wordsSplitsOnPunctuationAndDropsBlanks() {
        assertThat(Normalizer.words("show books under $20, please?"))
            .containsExactly("show", "books", "under", "20", "please");
    }

    @Test
    void stripPunctuationKeepsInnerHyphen() {
        assertThat(Normalizer.stripPunctuation("sci-fi?")).isEqualTo("sci-fi");
        assertThat(Normalizer.stripPunctuation("\"dune\"")).isEqualTo("dune");
    }
}
