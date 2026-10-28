import type { AuthProviderProps } from "react-oidc-context";

export const oidcConfig: AuthProviderProps = {
  authority: "http://localhost:8180/realms/orders",
  client_id: "orders-frontend",
  redirect_uri: window.location.origin,
  post_logout_redirect_uri: window.location.origin,
  onSigninCallback: () => {
    window.history.replaceState(null, "", window.location.pathname);
  },
};
