import { fetchApi } from "./apiClient";
import type { ConnectionDiagnosticsResponse } from "../types";

export const connectionApi = {
  getStatus: () =>
    fetchApi<ConnectionDiagnosticsResponse>("/worker/connection"),
};
