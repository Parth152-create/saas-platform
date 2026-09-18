import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Menu, Moon, Sun, X } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Button } from '../common/Button';
import { NexaLogo } from '../common/NexaLogo';

export const LandingNavbar: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const { resolvedTheme, toggleTheme } = useTheme();
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { label: 'Product', href: '#product' },
    { label: 'Platform', href: '#platform' },
    { label: 'Features', href: '#features' },
    { label: 'Security', href: '#security' },
    { label: 'Pricing', href: '#pricing' },
    { label: 'How It Works', href: '#how-it-works' },
  ];

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled
          ? 'bg-white/80 dark:bg-[#0a0a0a]/80 backdrop-blur-md border-b border-neutral-200/80 dark:border-[#262626]/80 shadow-xs'
          : 'bg-transparent border-b border-transparent'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link to="/" className="flex items-center select-none" aria-label="Nexa Home">
          <NexaLogo variant="full" size="md" />
        </Link>

        {/* Desktop Nav Links */}
        <nav className="hidden md:flex items-center gap-6">
          {navLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              className="text-xs font-medium text-neutral-600 dark:text-neutral-400 hover:text-neutral-950 dark:hover:text-neutral-100 transition-colors"
            >
              {link.label}
            </a>
          ))}
        </nav>

        {/* Desktop Actions */}
        <div className="hidden md:flex items-center gap-3">
          <button
            type="button"
            onClick={toggleTheme}
            className="p-2 rounded-lg text-neutral-500 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100 hover:bg-neutral-100 dark:hover:bg-[#1f1f1f] transition-colors cursor-pointer"
            aria-label="Toggle theme"
            title={resolvedTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            {resolvedTheme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>

          {isAuthenticated ? (
            <Link to="/app/dashboard">
              <Button size="sm" rightIcon={<ArrowRight className="w-3.5 h-3.5" />}>
                Go to Workspace
              </Button>
            </Link>
          ) : (
            <>
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  Login
                </Button>
              </Link>
              <Link to="/signup">
                <Button size="sm" rightIcon={<ArrowRight className="w-3.5 h-3.5" />}>
                  Get Started
                </Button>
              </Link>
            </>
          )}
        </div>

        {/* Mobile menu button */}
        <div className="flex items-center gap-2 md:hidden">
          <button
            type="button"
            onClick={toggleTheme}
            className="p-2 rounded-lg text-neutral-500 hover:text-neutral-900 dark:text-neutral-400 dark:hover:text-neutral-100"
            aria-label="Toggle theme"
          >
            {resolvedTheme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
          <button
            type="button"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="p-2 rounded-lg text-neutral-600 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-[#1a1a1a]"
            aria-label="Toggle menu"
          >
            {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile dropdown */}
      {isMobileMenuOpen && (
        <div className="md:hidden border-b border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#0a0a0a] px-4 pt-2 pb-6 space-y-3">
          <nav className="flex flex-col space-y-2">
            {navLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                onClick={() => setIsMobileMenuOpen(false)}
                className="px-3 py-2 rounded-lg text-sm font-medium text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-[#1a1a1a]"
              >
                {link.label}
              </a>
            ))}
          </nav>
          <div className="pt-3 border-t border-neutral-100 dark:border-[#262626] flex flex-col gap-2">
            {isAuthenticated ? (
              <Link to="/app/dashboard" onClick={() => setIsMobileMenuOpen(false)}>
                <Button className="w-full" rightIcon={<ArrowRight className="w-3.5 h-3.5" />}>
                  Go to Workspace
                </Button>
              </Link>
            ) : (
              <>
                <Link to="/login" onClick={() => setIsMobileMenuOpen(false)}>
                  <Button variant="outline" className="w-full">
                    Login
                  </Button>
                </Link>
                <Link to="/signup" onClick={() => setIsMobileMenuOpen(false)}>
                  <Button className="w-full" rightIcon={<ArrowRight className="w-3.5 h-3.5" />}>
                    Get Started
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
};
