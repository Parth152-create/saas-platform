import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  Briefcase,
  Building2,
  Calendar,
  CheckSquare,
  FolderKanban,
  Search as SearchIcon,
  Users,
  X,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent } from '../../components/common/Card';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { searchApi } from '../../api/searchApi';
import type { SearchResult } from '../../api/types';

export const SearchPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const queryParam = searchParams.get('q') || '';
  const typeParam = searchParams.get('type') || 'ALL';

  const [searchInput, setSearchInput] = useState(queryParam);
  const [selectedType, setSelectedType] = useState<string>(typeParam);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(Boolean(queryParam));
  const [hasSearched, setHasSearched] = useState(Boolean(queryParam));

  useEffect(() => {
    let isMounted = true;
    if (!queryParam.trim()) {
      return;
    }

    searchApi
      .search(queryParam.trim(), typeParam !== 'ALL' ? typeParam : undefined)
      .then((data) => {
        if (isMounted) {
          setResults(data);
          setHasSearched(true);
        }
      })
      .catch(() => {
        if (isMounted) {
          setResults([]);
          setHasSearched(true);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [queryParam, typeParam]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      setIsLoading(true);
      setSearchParams({
        q: searchInput.trim(),
        ...(selectedType !== 'ALL' ? { type: selectedType } : {}),
      });
    }
  };

  const handleTypeSelect = (type: string) => {
    setSelectedType(type);
    if (searchInput.trim()) {
      setIsLoading(true);
      setSearchParams({
        q: searchInput.trim(),
        ...(type !== 'ALL' ? { type } : {}),
      });
    }
  };

  const getEntityIcon = (type: string) => {
    switch (type) {
      case 'EMPLOYEE':
        return <Users className="w-4 h-4 text-blue-600 dark:text-blue-400" />;
      case 'PROJECT':
        return <FolderKanban className="w-4 h-4 text-purple-600 dark:text-purple-400" />;
      case 'TASK':
        return <CheckSquare className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />;
      case 'TEAM':
        return <Building2 className="w-4 h-4 text-amber-600 dark:text-amber-400" />;
      case 'LEAVE':
        return <Calendar className="w-4 h-4 text-rose-600 dark:text-rose-400" />;
      default:
        return <Briefcase className="w-4 h-4 text-neutral-500" />;
    }
  };

  const getEntityTypeBadge = (type: string) => {
    switch (type) {
      case 'EMPLOYEE':
        return <Badge size="sm" variant="info">Employee</Badge>;
      case 'PROJECT':
        return <Badge size="sm" variant="primary">Project</Badge>;
      case 'TASK':
        return <Badge size="sm" variant="success">Task</Badge>;
      case 'TEAM':
        return <Badge size="sm" variant="warning">Team</Badge>;
      case 'LEAVE':
        return <Badge size="sm" variant="danger">Leave</Badge>;
      default:
        return <Badge size="sm">{type}</Badge>;
    }
  };

  const filterChips = [
    { id: 'ALL', label: 'All Entities' },
    { id: 'EMPLOYEE', label: 'Employees' },
    { id: 'PROJECT', label: 'Projects' },
    { id: 'TASK', label: 'Tasks' },
    { id: 'TEAM', label: 'Teams' },
    { id: 'LEAVE', label: 'Leaves' },
  ];

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <PageHeader
        title="Global Workspace Search"
        description="Search across employees, projects, task boards, teams, and leave requests with role-based access filtering."
      />

      {/* Search Input Bar */}
      <form onSubmit={handleSearchSubmit} className="relative">
        <div className="relative flex items-center">
          <SearchIcon className="absolute left-4 w-5 h-5 text-neutral-400 pointer-events-none" />
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search anything (names, project titles, tasks, teams, leaves)..."
            className="w-full rounded-2xl border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] pl-12 pr-28 py-3.5 text-sm text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 shadow-xs transition-all focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:focus:border-white"
          />
          {searchInput && (
            <button
              type="button"
              onClick={() => {
                setSearchInput('');
                setResults([]);
                setHasSearched(false);
              }}
              className="absolute right-20 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-200 p-1"
            >
              <X className="w-4 h-4" />
            </button>
          )}
          <Button
            type="submit"
            size="sm"
            className="absolute right-2"
          >
            Search
          </Button>
        </div>
      </form>

      {/* Entity Filter Chips */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {filterChips.map((chip) => {
          const isSelected = selectedType === chip.id;
          return (
            <button
              key={chip.id}
              type="button"
              onClick={() => handleTypeSelect(chip.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors cursor-pointer ${
                isSelected
                  ? 'bg-neutral-900 text-white dark:bg-white dark:text-neutral-950'
                  : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200 dark:bg-[#1c1c1c] dark:text-neutral-400 dark:hover:bg-[#262626]'
              }`}
            >
              {chip.label}
            </button>
          );
        })}
      </div>

      {/* Search Results List */}
      <div className="space-y-3">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <LoadingSkeleton key={i} className="h-20 rounded-xl" />
          ))
        ) : results.length > 0 ? (
          <>
            <div className="text-xs text-neutral-400 font-medium px-1">
              Found {results.length} results for &quot;{searchInput}&quot;
            </div>
            {results.map((item) => (
              <Card
                key={`${item.type}-${item.id}`}
                className="hover:border-neutral-400 dark:hover:border-[#383838] transition-colors cursor-pointer"
                onClick={() => navigate(item.targetUrl)}
              >
                <CardContent className="p-4 flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3 min-w-0">
                    <div className="p-2 rounded-lg bg-neutral-100 dark:bg-[#1f1f1f] shrink-0 mt-0.5">
                      {getEntityIcon(item.type)}
                    </div>
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h4 className="text-sm font-bold text-neutral-900 dark:text-neutral-100 truncate">
                          {item.title}
                        </h4>
                        {getEntityTypeBadge(item.type)}
                        {item.status && (
                          <Badge variant="default" size="sm">
                            {item.status}
                          </Badge>
                        )}
                      </div>
                      {item.subtitle && (
                        <p className="text-xs font-medium text-neutral-500 dark:text-neutral-400 mt-0.5 truncate">
                          {item.subtitle}
                        </p>
                      )}
                      {item.description && (
                        <p className="text-xs text-neutral-400 dark:text-neutral-500 mt-1 line-clamp-2">
                          {item.description}
                        </p>
                      )}
                    </div>
                  </div>

                  <Button
                    variant="ghost"
                    size="sm"
                    className="shrink-0"
                    rightIcon={<ArrowRight className="w-3.5 h-3.5" />}
                  >
                    View
                  </Button>
                </CardContent>
              </Card>
            ))}
          </>
        ) : hasSearched ? (
          <div className="p-12 text-center text-xs text-neutral-500 bg-white dark:bg-[#141414] rounded-2xl border border-neutral-200 dark:border-[#262626]">
            <SearchIcon className="w-8 h-8 mx-auto text-neutral-400 mb-3 opacity-50" />
            No records matched your search query in this tenant workspace.
          </div>
        ) : (
          <div className="p-12 text-center text-xs text-neutral-400 bg-white dark:bg-[#141414] rounded-2xl border border-neutral-200 dark:border-[#262626]">
            Enter keywords above to search employees, active projects, tasks, teams, and leave requests.
          </div>
        )}
      </div>
    </div>
  );
};
