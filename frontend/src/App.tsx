import { useState } from "react";
import type { CartItem } from "./cart/types";
import { ProductList } from "./products/ProductList";
import type { Product } from "./products/api";
import { Cart } from "./cart/Cart";
import styles from "./App.module.css";

export function App() {
  const [cartItems, setCartItems] = useState<CartItem[]>([]);

  const handleAddToCart = (product: Product) => {
    setCartItems((prevItems) => {
      if (prevItems.some((item) => item.product.id === product.id)) {
        return prevItems.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item,
        );
      }

      return [...prevItems, { product, quantity: 1 }];
    });
  };

  const clearCart = () => setCartItems([]);

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Orders</h1>
      <section>
        <h2>Products</h2>
        <ProductList onAddToCart={handleAddToCart} />
      </section>
      <section className={styles.cartColumn}>
        <h2>Cart</h2>
        <Cart items={cartItems} onOrderPlaced={clearCart} />
      </section>
    </main>
  );
}
