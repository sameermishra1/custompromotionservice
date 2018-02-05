# custompromotionservice

Extend the OOTB promotion services to provide features like promotional prices on PDP.

Features :

get the promotional prices for a product : PriceData promotionalPrice = ddPromotionEngineService.extractPromotionalPriceWithoutCart(productData); make sure that the productData has the price information available, for that when creating the ProductData from ProductModel do use this option ProductOption.PRICE_RANGE.
