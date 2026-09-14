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

    private boolean isValid(String value) {
        return isValid(value, false);
    }

    private boolean isValid(String value, boolean required) {
        final UrlValidator validator = createUrlValidator(value, required);
        validator.performValidation();
        return validator.isValid();
    }

    private static UrlValidator createUrlValidator(final String value, final boolean required) {
        final UrlValidator validator = new UrlValidator();
        validator.setRequired(required);
        final HtmlChainValidator chain = new HtmlChainValidator(new TestValidatable(value));
        chain.addValidator(validator);
        return validator;
    }

    @Test
    public void givenDefaultValidator_thenRequiredIsTrue() {
        assertTrue(new UrlValidator().isRequired());
    }

    @Test
    public void givenCompleteHttpUrl_isValid() {
        assertTrue(isValid("http://example.com"));
    }

    @Test
    public void givenCompleteHttpsUrl_isValid() {
        assertTrue(isValid("https://www.example.com/sub/page?q=1#frag"));
    }

    @Test
    public void givenUrlWithoutScheme_isValid() {
        assertTrue(isValid("example.com"));
    }

    @Test
    public void givenUrlWithoutSchemeWithPortAndPath_isValid() {
        assertTrue(isValid("example.com:8080/path"));
    }

    @Test
    public void givenIpAddressUrl_isValid() {
        assertTrue(isValid("http://192.168.1.1:8080/page"));
    }

    @Test
    public void givenMalformedUrlWithSpaces_isInvalid() {
        assertFalse(isValid("http://exa mple.com"));
    }

    @Test
    public void givenMalformedUrlWithoutScheme_isInvalid() {
        assertFalse(isValid("not a url"));
    }

    @Test
    public void givenSingleLabelHost_isInvalid() {
        assertFalse(isValid("foo"));
    }

    @Test
    public void givenMissingHost_isInvalid() {
        assertFalse(isValid("http://"));
    }

    @Test
    public void givenSchemeNameOnly_isInvalid() {
        assertFalse(isValid("http"));
        assertFalse(isValid("https"));
    }

    @Test
    public void givenUppercaseSchemeUrl_isInvalid() {
        assertFalse(isValid("HTTP://EXAMPLE.COM"));
    }

    @Test
    public void givenRequiredAndEmptyValue_isInvalid() {
        assertFalse(isValid("", true));
    }

    @Test
    public void givenRequiredAndNullValue_isInvalid() {
        assertFalse(isValid(null, true));
    }

    @Test
    public void givenNotRequiredAndEmptyValue_isValid() {
        assertTrue(isValid("", false));
    }

    @Test
    public void givenNotRequiredAndNullValue_isValid() {
        assertTrue(isValid(null, false));
    }

    private record TestValidatable(String value) implements Validatable {

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String[] getValues() {
            return new String[] { value };
        }

        @Override
        public void addValidator(HtmlValidator validator) {
        }

        @Override
        public HtmlChainValidator getChainValidator() {
            return null;
        }

        @Override
        public void setChainValidator(HtmlChainValidator chainValidator) {
        }
    }

}