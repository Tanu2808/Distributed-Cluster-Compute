import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { config } from '../utils/config';
import { queryKeys } from '../types/api';
import { fetchSettings, updateSettings } from '../api/settingsApi';
import { mockSettings } from '../mock/settings';
import type { ClusterSettings } from '../types';

/**
 * Returns the current cluster settings.
 *
 * Mock mode:  returns mockSettings immediately.
 * Real mode:  fetches GET /api/settings.
 */
export function useSettings() {
  return useQuery<ClusterSettings, Error>({
    queryKey: queryKeys.settings,
    queryFn: config.USE_MOCK_API
      ? () => Promise.resolve(mockSettings)
      : fetchSettings,
    staleTime: config.USE_MOCK_API ? Infinity : 30_000, // settings change infrequently
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
    mutationFn: config.USE_MOCK_API
      ? (settings) =>
          new Promise<ClusterSettings>((resolve) =>
            setTimeout(() => resolve(settings), 600),
          )
      : updateSettings,

    onSuccess: (savedSettings) => {
      // Update the settings cache with the confirmed data from the server
      queryClient.setQueryData<ClusterSettings>(queryKeys.settings, savedSettings);
    },
  });
}
