package dev.tengiz.payment.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    private static final Set<String> VALID_CURRENCIES = Currency.getAvailableCurrencies()
        .stream()
        .map(Currency::getCurrencyCode)
        .collect(Collectors.toSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null &&
            value.length() == 3 &&
            VALID_CURRENCIES.contains(value.toUpperCase());
    }
}
