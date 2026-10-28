import { useState } from "react";
import type { CartItem } from "./cart/types";
import { ProductList } from "./products/ProductList";
import type { Product } from "./products/api";
import { Cart } from "./cart/Cart";
import styles from "./App.module.css";
import { useAuth } from "react-oidc-context";
import { MyOrders } from "./orders/MyOrders";

type View = "shop" | "orders";

export function App() {
  const [view, setView] = useState<View>("shop");
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
        <div className={styles.brand}>
          <h1 className={styles.title}>Orders</h1>
          <nav>
            <button
              type="button"
              className={styles.navButton}
              onClick={() => setView("shop")}
              aria-current={view === "shop" ? "page" : undefined}
            >
              Shop
            </button>
            {auth.isAuthenticated && (
              <button
                type="button"
                className={styles.navButton}
                onClick={() => setView("orders")}
                aria-current={view === "orders" ? "page" : undefined}
              >
                My orders
              </button>
            )}
          </nav>
        </div>
        <div className={styles.account}>{accountContents}</div>
      </header>
      {view === "orders" && auth.isAuthenticated ? (
        <main className={styles.ordersPage}>
          <h2>My orders</h2>
          <MyOrders />
        </main>
      ) : (
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
      )}
    </>
  );
}
