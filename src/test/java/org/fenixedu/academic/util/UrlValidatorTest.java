package org.fenixedu.academic.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.FenixFrameworkRunner;

import pt.ist.fenixWebFramework.renderers.components.Validatable;
import pt.ist.fenixWebFramework.renderers.validators.HtmlChainValidator;
import pt.ist.fenixWebFramework.renderers.validators.HtmlValidator;

@RunWith(FenixFrameworkRunner.class)
public class UrlValidatorTest {

    private static final class TestValidatable implements Validatable {

        private final String value;

        TestValidatable(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String[] getValues() {
            return new String[] { value };
        }

        @Override
        public void setChainValidator(HtmlChainValidator chainValidator) {
        }

        @Override
        public void addValidator(HtmlValidator validator) {
        }

        @Override
        public HtmlChainValidator getChainValidator() {
            return null;
        }
    }

    private UrlValidator validatorFor(String value) {
        return validatorFor(value, true);
    }

    private UrlValidator validatorFor(String value, boolean required) {
        final HtmlChainValidator chain = new HtmlChainValidator(new TestValidatable(value));
        final UrlValidator validator = new UrlValidator();
        validator.setRequired(required);
        chain.addValidator(validator);
        return validator;
    }

    @Test
    public void givenCompleteHttpUrl_whenValidating_thenValid() {
        final UrlValidator validator = validatorFor("http://example.com");

        validator.performValidation();

        assertTrue(validator.isValid());
    }

    @Test
    public void givenCompleteHttpsUrl_whenValidating_thenValid() {
        final UrlValidator validator = validatorFor("https://www.example.com/sub/page?q=1#frag");

        validator.performValidation();

        assertTrue(validator.isValid());
    }

    @Test
    public void givenUrlWithoutScheme_whenValidating_thenValid() {
        final UrlValidator validator = validatorFor("example.com");

        validator.performValidation();

        assertTrue(validator.isValid());
    }

    @Test
    public void givenUrlWithoutSchemeWithPortAndPath_whenValidating_thenValid() {
        final UrlValidator validator = validatorFor("example.com:8080/path");

        validator.performValidation();

        assertTrue(validator.isValid());
    }

    @Test
    public void givenIpAddressUrl_whenValidating_thenValid() {
        final UrlValidator validator = validatorFor("http://192.168.1.1:8080/page");

        validator.performValidation();

        assertTrue(validator.isValid());
    }

    @Test
    public void givenMalformedUrlWithSpaces_whenValidating_thenInvalid() {
        final UrlValidator validator = validatorFor("http://exa mple.com");

        validator.performValidation();

        assertFalse(validator.isValid());
    }

    @Test
    public void givenMalformedUrlWithoutScheme_whenValidating_thenInvalid() {
        final UrlValidator validator = validatorFor("not a url");

        validator.performValidation();

        assertFalse(validator.isValid());
    }

    @Test
    public void givenSingleLabelHost_whenValidating_thenInvalid() {
        final UrlValidator validator = validatorFor("foo");

        validator.performValidation();

        assertFalse(validator.isValid());
    }

    @Test
    public void givenMissingHost_whenValidating_thenInvalid() {
        final UrlValidator validator = validatorFor("http://");

        validator.performValidation();

        assertFalse(validator.isValid());
    }

    @Test
    public void givenSchemeNameOnly_whenValidating_thenInvalid() {
        final UrlValidator http = validatorFor("http");
        final UrlValidator https = validatorFor("https");

        http.performValidation();
        https.performValidation();

        assertFalse(http.isValid());
        assertFalse(https.isValid());
    }

    @Test
    public void givenUppercaseSchemeUrl_whenValidating_thenInvalid() {
        final UrlValidator validator = validatorFor("HTTP://EXAMPLE.COM");

        validator.performValidation();

        assertFalse(validator.isValid());
    }

    @Test
    public void givenRequiredAndEmptyValue_whenValidating_thenInvalid() {
        final UrlValidator validator = validatorFor("", true);

        validator.performValidation();

        assertFalse(validator.isValid());
    }

    @Test
    public void givenRequiredAndNullValue_whenValidating_thenInvalid() {
        final UrlValidator validator = validatorFor(null, true);

        validator.performValidation();

        assertFalse(validator.isValid());
    }

    @Test
    public void givenNotRequiredAndEmptyValue_whenValidating_thenValid() {
        final UrlValidator validator = validatorFor("", false);

        validator.performValidation();

        assertTrue(validator.isValid());
    }

    @Test
    public void givenNotRequiredAndNullValue_whenValidating_thenValid() {
        final UrlValidator validator = validatorFor(null, false);

        validator.performValidation();

        assertTrue(validator.isValid());
    }

    @Test
    public void givenDefaultValidator_thenRequiredIsTrue() {
        assertTrue(new UrlValidator().isRequired());
    }
}