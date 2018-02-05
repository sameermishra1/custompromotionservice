package com.dd.promotions.promotionengineservices.converter.populator;

import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.ruleengineservices.rao.CartRAO;
import de.hybris.platform.ruleengineservices.rao.OrderEntryRAO;
import de.hybris.platform.ruleengineservices.rao.ProductRAO;
import de.hybris.platform.ruleengineservices.rao.UserRAO;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.user.UserService;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class PromotionalPriceCartRaoPopulator<T extends ProductData, P extends CartRAO> implements Populator<T, P> {

  //TODO : analysis, raise the cieling to a higher number so that all the promotions are fired
  private static int QUANTITY_UPPER_CIELING = 1;

  @Autowired
  @Qualifier("userRaoConverter")
  private Converter<UserModel, UserRAO> userConverter;
  @Autowired
  @Qualifier("productRaoConverter")
  private Converter<ProductModel, ProductRAO> productConverter;
  @Autowired
  private ProductService productService;

  @Autowired
  private UserService userService;

  public void populate(T source, P target) {
    if (target.getActions() == null) {
      target.setActions(new LinkedHashSet());
    }
    target.setCode(source.getCode());
    setPriceAndCurrency(source, target);
    populateEntryDetail(source, target);
    convertAndSetUser(target);
    target.setActions(new LinkedHashSet());
    target.setOriginalTotal(target.getTotal());
  }

  private void setPriceAndCurrency(T source, P target) {
    PriceData priceInfo = source.getPrice();
    BigDecimal cost = BigDecimal.ZERO;
    if (priceInfo != null) {
      if (StringUtils.isNotEmpty(priceInfo.getCurrencyIso())) {
        target.setCurrencyIsoCode(priceInfo.getCurrencyIso());
      }
      cost = (priceInfo.getValue() != null) ? priceInfo.getValue().multiply(BigDecimal.valueOf(QUANTITY_UPPER_CIELING)) : BigDecimal.ZERO;
    }
    target.setTotal(cost);
    target.setSubTotal(cost);
    target.setDeliveryCost(BigDecimal.ZERO);
  }

  private void convertAndSetUser(P target) {
    UserModel user = userService.getCurrentUser();
    if (user != null && (user instanceof CustomerModel)) {
      target.setUser(userConverter.convert(user));
    }
  }


  private void populateEntryDetail(T source, P target) {

    final Set cartEntries = new LinkedHashSet();
    OrderEntryRAO entry = new OrderEntryRAO();
    final ProductModel product = productService.getProductForCode(source.getCode());
    entry.setProduct(productConverter.convert(product));
    entry.setQuantity(1);
//TODO : check base price value
    entry.setBasePrice(target.getTotal());
    target.setCurrencyIsoCode(target.getCurrencyIsoCode());
    entry.setEntryNumber(QUANTITY_UPPER_CIELING);
    entry.setOrder(target);
    cartEntries.add(entry);
    target.setEntries(cartEntries);
  }

}
