import { apiClient } from './apiClient';
import type { SearchResult } from './types';

export const searchApi = {
  search: (query: string, type?: string, limit?: number): Promise<SearchResult[]> => {
    const qs = new URLSearchParams();
    qs.set('q', query);
    if (type && type !== 'ALL') qs.set('type', type);
    if (limit) qs.set('limit', limit.toString());
    return apiClient.get<SearchResult[]>(`/api/search?${qs.toString()}`);
  },
};
