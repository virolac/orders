import { type Product } from "./api";

const priceFormatter = new Intl.NumberFormat(undefined, {
  style: "currency",
  currency: "EUR",
});

export type ProductCardProps = {
  product: Product;
};

export function ProductCard({ product }: ProductCardProps) {
  return (
    <article>
      <h2>{product.name}</h2>
      {product.imageUrl !== null ? (
        <img
          src={product.imageUrl}
          alt={product.name}
          loading="lazy"
          width="200"
          height="200"
        />
      ) : (
        <div>No image</div>
      )}
      {product.description && <p>{product.description}</p>}
      <span>{priceFormatter.format(product.price)}</span>
    </article>
  );
}
