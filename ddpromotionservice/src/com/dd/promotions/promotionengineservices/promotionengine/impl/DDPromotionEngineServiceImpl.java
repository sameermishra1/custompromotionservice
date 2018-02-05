package com.dd.promotions.promotionengineservices.promotionengine.impl;

import com.dd.promotions.promotionengineservices.promotionengine.DDPromotionEngineService;
import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.promotionengineservices.promotionengine.impl.DefaultPromotionEngineService;
import de.hybris.platform.promotions.model.PromotionGroupModel;
import de.hybris.platform.ruleengine.RuleEvaluationContext;
import de.hybris.platform.ruleengine.RuleEvaluationResult;
import de.hybris.platform.ruleengine.enums.RuleType;
import de.hybris.platform.ruleengine.model.AbstractRuleEngineContextModel;
import de.hybris.platform.ruleengineservices.enums.FactContextType;
import de.hybris.platform.ruleengineservices.rao.OrderEntryConsumedRAO;
import de.hybris.platform.servicelayer.time.TimeService;
import de.hybris.platform.site.BaseSiteService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class DDPromotionEngineServiceImpl extends DefaultPromotionEngineService implements DDPromotionEngineService {

  @Autowired
  CartService cartService;
  @Autowired
  private ProductService productService;
  @Autowired
  private BaseSiteService baseSiteService;
  @Autowired
  private TimeService timeService;
  @Autowired
  private PriceDataFactory priceDataFactory;

  @Override
  public PriceData extractPromotionalPriceWithoutCart(ProductData product) {
    Object perSessionLock = getSessionService().getOrLoadAttribute("promotionsUpdateLock", () ->
        new DDPromotionEngineServiceImpl.SerializableObject());
    synchronized (perSessionLock) {
      return evaluatePromotionalPriceForProduct(product, getPromotionGroups(), timeService.getCurrentTime());
    }
  }

  // TODO :enhance it to run in a separate context
  private PriceData evaluatePromotionalPriceForProduct(ProductData productData, Collection<PromotionGroupModel> promotionGroups,
      Date date) {
    List<Object> facts = new ArrayList();
    facts.add(productData);
    facts.addAll(promotionGroups);
    facts.add(date);
    RuleEvaluationContext context = prepareContext(getFactContextFactory().createFactContext(FactContextType.PRODUCT_PROMOTION_PRICE, facts),
        determinePromotionalPriceRuleEngineContext(productData));
    RuleEvaluationResult ruleEvaluationResult = getCommerceRuleEngineService().evaluate(context);
    return extractPromotionalPrice(ruleEvaluationResult, productData);
  }

  //TODO : more analysis, using the same context can have an impact on the RRD
  protected AbstractRuleEngineContextModel determinePromotionalPriceRuleEngineContext(ProductData productData) {
    final ProductModel productModel = productService.getProductForCode(productData.getCode());
    return getRuleEngineContextFinderStrategy().findRuleEngineContext(productModel, RuleType.PROMOTION).orElseThrow(() ->
        new IllegalStateException(String.format("No rule engine context could be derived for product [%s]", productModel.getCode())));
  }

  protected Collection<PromotionGroupModel> getPromotionGroups() {
    final Collection<PromotionGroupModel> promotionGroupModels = new ArrayList<PromotionGroupModel>();
    if (baseSiteService.getCurrentBaseSite() != null
        && baseSiteService.getCurrentBaseSite().getDefaultPromotionGroup() != null) {
      promotionGroupModels.add(baseSiteService.getCurrentBaseSite().getDefaultPromotionGroup());
    }
    return promotionGroupModels;
  }

  private PriceData extractPromotionalPrice(RuleEvaluationResult ruleEvaluationResult, ProductData product) {
    if (!ruleEvaluationResult.isEvaluationFailed() && CollectionUtils.isNotEmpty(ruleEvaluationResult.getResult().getActions())) {
      Optional<BigDecimal> promotionalPrice = ruleEvaluationResult.getResult().getActions().stream().map(x -> {
        if (CollectionUtils.isNotEmpty(x.getConsumedEntries())) {
          Optional<OrderEntryConsumedRAO> consumedEntry = x.getConsumedEntries().stream().findFirst();
          if (consumedEntry.isPresent()) {
            return consumedEntry.get().getAdjustedUnitPrice();
          }
        }
        return BigDecimal.ZERO;
      }).filter(x -> x.compareTo(BigDecimal.ZERO) > 0).min(Comparator.naturalOrder());

      if (promotionalPrice.isPresent()) {
        return
            getPromotionalPriceData(promotionalPrice.get(), product.getPrice().getCurrencyIso(), product.getPrice().getPriceType());
      }
    }
    return new PriceData();
  }

  private PriceData getPromotionalPriceData(BigDecimal price, String currencyIso, PriceDataType priceType) {
    final PriceData priceData = priceDataFactory.create(priceType, price, currencyIso);
    return priceData;
  }

  private static class SerializableObject implements Serializable {

    private SerializableObject() {
    }
  }
}
