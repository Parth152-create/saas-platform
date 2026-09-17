import { useState } from 'react';
import { Sliders, Terminal } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { apiClient } from '../../api/apiClient';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';

export const SystemSettingsPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [rbacTestOutput, setRbacTestOutput] = useState<string | null>(null);
  const [testingEndpoint, setTestingEndpoint] = useState<string | null>(null);

  const testRbacEndpoint = async (level: 'user' | 'manager' | 'admin' | 'super-admin') => {
    setTestingEndpoint(level);
    try {
      const res = await apiClient.get<string>(`/api/rbac-debug/${level}`);
      setRbacTestOutput(`✅ HTTP 200 OK: ${typeof res === 'string' ? res : JSON.stringify(res)}`);
      showToast('success', 'RBAC Authorized', `Access granted for level: ${level}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Request failed';
      setRbacTestOutput(`❌ Denied: ${msg}`);
      showToast('error', 'RBAC Restricted', msg);
    } finally {
      setTestingEndpoint(null);
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Sliders className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />
            <span>Super Administrator System Settings</span>
          </CardTitle>
          <p className="text-xs text-neutral-500 mt-1">
            Low-level multi-tenant database diagnostics and live Spring Security RBAC verification
          </p>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Tenant Schema Info */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] space-y-1">
              <span className="text-[10px] font-bold uppercase tracking-wider text-neutral-400">
                Current PostgreSQL Schema
              </span>
              <p className="font-mono font-bold text-sm text-neutral-900 dark:text-neutral-100">
                tenant_{user?.tenantId}
              </p>
            </div>
            <div className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] space-y-1">
              <span className="text-[10px] font-bold uppercase tracking-wider text-neutral-400">
                Flyway Tenant Migrations
              </span>
              <p className="font-mono font-bold text-sm text-neutral-900 dark:text-neutral-100">
                V9__add_invite_token
              </p>
            </div>
            <div className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] space-y-1">
              <span className="text-[10px] font-bold uppercase tracking-wider text-neutral-400">
                Backend Port & Host
              </span>
              <p className="font-mono font-bold text-sm text-neutral-900 dark:text-neutral-100">
                http://localhost:8090
              </p>
            </div>
          </div>

          {/* RBAC Debug Tester */}
          <div className="p-5 rounded-xl border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#141414] space-y-4">
            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-neutral-700 dark:text-neutral-300 flex items-center gap-2">
                <Terminal className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
                <span>Live Backend RBAC Verification Tester</span>
              </h4>
              <p className="text-xs text-neutral-500 mt-1">
                Trigger real requests to <code className="font-mono">/api/rbac-debug/*</code> to verify your active session authorities against Spring Security RoleHierarchy.
              </p>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                variant="outline"
                size="sm"
                isLoading={testingEndpoint === 'user'}
                onClick={() => testRbacEndpoint('user')}
              >
                Test @PreAuthorize(&quot;hasRole(&apos;USER&apos;)&quot;)
              </Button>
              <Button
                variant="outline"
                size="sm"
                isLoading={testingEndpoint === 'manager'}
                onClick={() => testRbacEndpoint('manager')}
              >
                Test @PreAuthorize(&quot;hasRole(&apos;MANAGER&apos;)&quot;)
              </Button>
              <Button
                variant="outline"
                size="sm"
                isLoading={testingEndpoint === 'admin'}
                onClick={() => testRbacEndpoint('admin')}
              >
                Test @PreAuthorize(&quot;hasRole(&apos;ADMIN&apos;)&quot;)
              </Button>
              <Button
                variant="outline"
                size="sm"
                isLoading={testingEndpoint === 'super-admin'}
                onClick={() => testRbacEndpoint('super-admin')}
              >
                Test @PreAuthorize(&quot;hasRole(&apos;SUPER_ADMIN&apos;)&quot;)
              </Button>
            </div>

            {rbacTestOutput && (
              <div className="p-3 bg-neutral-950 text-neutral-100 dark:bg-[#0a0a0a] rounded-lg font-mono text-xs overflow-x-auto border border-neutral-800 dark:border-[#262626]">
                {rbacTestOutput}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
