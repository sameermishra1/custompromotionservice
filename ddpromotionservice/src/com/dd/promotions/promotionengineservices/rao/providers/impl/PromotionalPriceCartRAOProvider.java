package com.dd.promotions.promotionengineservices.rao.providers.impl;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.ruleengineservices.calculation.RuleEngineCalculationService;
import de.hybris.platform.ruleengineservices.rao.CartRAO;
import de.hybris.platform.ruleengineservices.rao.OrderEntryRAO;
import de.hybris.platform.ruleengineservices.rao.ProductRAO;
import de.hybris.platform.ruleengineservices.rao.UserRAO;
import de.hybris.platform.ruleengineservices.rao.providers.impl.AbstractExpandedRAOProvider;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class PromotionalPriceCartRAOProvider extends AbstractExpandedRAOProvider<ProductData, CartRAO> {

  private final Collection<String> defaultOptions = Arrays
      .asList("INCLUDE_CART", "EXPAND_ENTRIES", "EXPAND_PRODUCTS", "EXPAND_CATEGORIES", "EXPAND_USERS");

  @Autowired
  private Converter<ProductData, CartRAO> promotionalPriceCartRAOConverter;
  @Autowired
  private RuleEngineCalculationService ruleEngineCalculationService;

  @Override
  protected CartRAO createRAO(ProductData product) {
    CartRAO rao = promotionalPriceCartRAOConverter.convert(product);
    ruleEngineCalculationService.calculateTotals(rao);
    return rao;
  }

  @Override
  protected Set expandRAO(CartRAO cart, Collection<String> options) {
    final Set facts = new LinkedHashSet();
    facts.addAll(super.expandRAO(cart, options));
    options.stream().forEach(x -> handleRAOOptions(x, cart, facts));
    return facts;
  }


  private void handleRAOOptions(String option, CartRAO cart, Set facts) {
    Set<OrderEntryRAO> entries = cart.getEntries();
    switch (option) {
      case "INCLUDE_CART":
        facts.add(cart);
        break;
      case "EXPAND_PRODUCTS":
        addProducts(facts, entries);
        break;
      case "EXPAND_USERS":
        addUserGroups(facts, cart.getUser());
        break;
      case "EXPAND_CATEGORIES":
        addProductCategories(facts, entries);
        break;
      case "EXPAND_ENTRIES":
        addEntries(facts, entries);
    }
  }

  private void addProductCategories(Set<Object> facts, Set<OrderEntryRAO> entries) {
    entries.stream().forEach(x -> {
      ProductRAO product = x.getProduct();
      if (CollectionUtils.isNotEmpty(product.getCategories())) {
        facts.addAll(product.getCategories());
      }
    });
  }

  private void addUserGroups(Set<Object> facts, UserRAO userRAO) {
    if (userRAO != null) {
      facts.add(userRAO);
      Set groups = userRAO.getGroups();
      if (CollectionUtils.isNotEmpty(groups)) {
        facts.addAll(groups);
      }
    }
  }

  private void addProducts(Set<Object> facts, Set<OrderEntryRAO> entries) {
    if (CollectionUtils.isNotEmpty(entries)) {
      entries.forEach(x -> {
        facts.add(x.getProduct());
      });
    }
  }

  private void addEntries(Set<Object> facts, Set<OrderEntryRAO> entries) {
    if (CollectionUtils.isNotEmpty(entries)) {
      facts.addAll(entries);
    }
  }

  @Override
  protected Collection<String> getDefaultOptions() {
    return this.defaultOptions;
  }

  @Override
  protected Collection<String> getValidOptions() {
    return this.defaultOptions;
  }

  @Override
  protected Collection<String> getMinOptions() {
    return this.defaultOptions;

  }
}
