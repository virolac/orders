ALTER TABLE product
ADD COLUMN description VARCHAR(1000),
ADD COLUMN image_url VARCHAR(2048);

UPDATE product
SET description = 'The Essence Mascara Lash Princess is a popular mascara known for its volumizing and lengthening effects. Achieve dramatic lashes with this long-lasting and cruelty-free formula.'
  , image_url = 'https://cdn.dummyjson.com/product-images/beauty/essence-mascara-lash-princess/thumbnail.webp'
WHERE name = 'Essence Mascara Lash Princess';

UPDATE product
SET description = 'The Powder Canister is a finely milled setting powder designed to set makeup and control shine. With a lightweight and translucent formula, it provides a smooth and matte finish.'
  , image_url = 'https://cdn.dummyjson.com/product-images/beauty/powder-canister/thumbnail.webp'
WHERE name = 'Powder Canister';

UPDATE product
SET description = 'The Red Lipstick is a classic and bold choice for adding a pop of color to your lips. With a creamy and pigmented formula, it provides a vibrant and long-lasting finish.'
  , image_url = 'https://cdn.dummyjson.com/product-images/beauty/red-lipstick/thumbnail.webp'
WHERE name = 'Red Lipstick';
