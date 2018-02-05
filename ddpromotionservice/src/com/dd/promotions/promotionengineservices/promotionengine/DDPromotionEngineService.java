package com.dd.promotions.promotionengineservices.promotionengine;

import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.promotionengineservices.promotionengine.PromotionEngineService;

public interface DDPromotionEngineService extends PromotionEngineService {
   PriceData extractPromotionalPriceWithoutCart(ProductData product);
}
