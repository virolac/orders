import { formatPrice } from "../format";
import { type Product } from "./api";

export type ProductCardProps = {
  product: Product;
  onAddToCart: (product: Product) => void;
};

export function ProductCard({ product, onAddToCart }: ProductCardProps) {
  return (
    <article>
      <h3>{product.name}</h3>
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
      <span>{formatPrice(product.price)}</span>
      <button type="button" onClick={() => onAddToCart(product)}>
        Add to cart
      </button>
    </article>
  );
}
