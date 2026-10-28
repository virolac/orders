import { useState } from "react";
import type { CartItem } from "./cart/types";
import { ProductList } from "./products/ProductList";
import type { Product } from "./products/api";
import { Cart } from "./cart/Cart";
import styles from "./App.module.css";
import { useAuth } from "react-oidc-context";

export function App() {
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const auth = useAuth();

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

  let accountContents;
  if (auth.isLoading) {
    accountContents = <span>Signing in...</span>;
  } else if (auth.isAuthenticated && auth.user) {
    accountContents = (
      <>
        <span>{auth.user.profile.preferred_username}</span>
        <button type="button" onClick={() => auth.signoutRedirect()}>
          Log out
        </button>
      </>
    );
  } else {
    accountContents = (
      <button type="button" onClick={() => auth.signinRedirect()}>
        Log in
      </button>
    );
  }

  return (
    <>
      <header className={styles.header}>
        <h1 className={styles.title}>Orders</h1>
        <div className={styles.account}>{accountContents}</div>
      </header>
      <main className={styles.page}>
        <section>
          <h2>Products</h2>
          <ProductList onAddToCart={handleAddToCart} />
        </section>
        <section className={styles.cartColumn}>
          <h2>Cart</h2>
          <Cart items={cartItems} onOrderPlaced={clearCart} />
        </section>
      </main>
    </>
  );
}
