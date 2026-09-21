import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "../types/api";
import { fetchSettings, updateSettings } from "../api/settingsApi";
import type { ClusterSettings } from "../types";

/**
 * Returns the current cluster settings.
 *
 * Mock mode:  returns mockSettings immediately.
 * Real mode:  fetches GET /api/settings.
 */
export function useSettings() {
  return useQuery<ClusterSettings, Error>({
    queryKey: queryKeys.settings,
    queryFn: fetchSettings,
    staleTime: 30_000, // settings change infrequently
  });
}

/**
 * Returns a mutation for updating cluster settings via PUT /api/settings.
 *
 * Mock mode:  simulates a 600ms network delay, then resolves with the submitted settings.
 * Real mode:  calls PUT /api/settings and updates the cache on success.
 *
 * Usage:
 *   const { mutate, isPending, isSuccess, isError } = useUpdateSettings();
 *   mutate(newSettings);
 */
export function useUpdateSettings() {
  const queryClient = useQueryClient();

  return useMutation<ClusterSettings, Error, ClusterSettings>({
    mutationFn: updateSettings,

    onSuccess: (savedSettings) => {
      // Update the settings cache with the confirmed data from the server
      queryClient.setQueryData<ClusterSettings>(
        queryKeys.settings,
        savedSettings,
      );
    },
  });
}
