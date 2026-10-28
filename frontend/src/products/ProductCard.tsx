import { formatPrice } from "../format";
import { type Product } from "./api";
import styles from "./ProductCard.module.css";

export type ProductCardProps = {
  product: Product;
  onAddToCart: (product: Product) => void;
};

export function ProductCard({ product, onAddToCart }: ProductCardProps) {
  return (
    <article className={styles.card}>
      <h3 className={styles.name}>{product.name}</h3>
      {product.imageUrl !== null ? (
        <img
          src={product.imageUrl}
          alt={product.name}
          loading="lazy"
          width="200"
          height="200"
          className={styles.image}
        />
      ) : (
        <div className={styles.placeholder}>No image</div>
      )}
      {product.description && (
        <p className={styles.description} title={product.description}>
          {product.description}
        </p>
      )}
      <span className={styles.price}>{formatPrice(product.price)}</span>
      <button type="button" onClick={() => onAddToCart(product)}>
        Add to cart
      </button>
    </article>
  );
}
