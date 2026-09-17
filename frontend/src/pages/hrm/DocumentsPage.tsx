import { Download, FileText, Plus, ShieldCheck } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { useToast } from '../../context/ToastContext';

export const DocumentsPage: React.FC = () => {
  const { showToast } = useToast();

  const documents = [
    {
      id: 'doc-1',
      name: 'Standard Employee NDA & Confidentiality Agreement.pdf',
      category: 'Legal & Compliance',
      size: '2.4 MB',
      updatedAt: '2026-08-15',
      access: 'All Staff',
    },
    {
      id: 'doc-2',
      name: '2026 Company Policy & Handbook v4.2.pdf',
      category: 'Human Resources',
      size: '5.1 MB',
      updatedAt: '2026-09-01',
      access: 'All Staff',
    },
    {
      id: 'doc-3',
      name: 'SOC2 Type II Audit Executive Summary.pdf',
      category: 'Security',
      size: '1.8 MB',
      updatedAt: '2026-07-20',
      access: 'Admin & Leadership',
    },
    {
      id: 'doc-4',
      name: 'Work From Home & Equipment Policy.pdf',
      category: 'Operations',
      size: '850 KB',
      updatedAt: '2026-06-11',
      access: 'All Staff',
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Employee Documents Vault"
        description="Centralized company policies, employee agreements, and security compliance certificates."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'Upload Document', 'Document upload dialog opened')}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Upload Document
          </Button>
        }
      />

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <FileText className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
            <span>Organization Document Library</span>
          </CardTitle>
          <div className="flex items-center gap-1 text-xs text-zinc-400">
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
            <span>Encrypted at Rest</span>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Document Title</th>
                  <th className="px-5 py-3">Category</th>
                  <th className="px-5 py-3">File Size</th>
                  <th className="px-5 py-3">Last Updated</th>
                  <th className="px-5 py-3">Access Tier</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {documents.map((doc) => (
                  <tr
                    key={doc.id}
                    className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                  >
                    <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100 flex items-center gap-2">
                      <FileText className="w-4 h-4 text-neutral-400 shrink-0" />
                      <span>{doc.name}</span>
                    </td>
                    <td className="px-5 py-3.5">{doc.category}</td>
                    <td className="px-5 py-3.5 font-mono">{doc.size}</td>
                    <td className="px-5 py-3.5">{doc.updatedAt}</td>
                    <td className="px-5 py-3.5">
                      <Badge variant="default" size="sm">
                        {doc.access}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => showToast('success', 'Downloading file', doc.name)}
                        leftIcon={<Download className="w-3.5 h-3.5" />}
                      >
                        Download
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
