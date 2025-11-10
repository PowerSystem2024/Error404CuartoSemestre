import { useState, useEffect, ReactNode } from 'react';
import { useLocation } from 'react-router-dom';

// Global storage for scroll positions
const SCROLL_POSITIONS: Record<string, number> = {};

interface ScrollMemoryProps {
    children: ReactNode;
}

/**
 * ScrollMemory component that remembers the scroll position for a specific route
 * and restores it when the user navigates back to that route.
 */
const ScrollMemory = ({ children }: ScrollMemoryProps) => {
    const { pathname } = useLocation();
    const [isRestored, setIsRestored] = useState(false);

    // When route changes, save the current scroll position
    useEffect(() => {
        const savePosition = () => {
            // Save current scroll position before unmounting
            SCROLL_POSITIONS[pathname] = window.scrollY;
        };

        // Add event listener for beforeunload
        window.addEventListener('beforeunload', savePosition);

        // Save position when component unmounts or route changes
        return () => {
            window.removeEventListener('beforeunload', savePosition);
            SCROLL_POSITIONS[pathname] = window.scrollY;
        };
    }, [pathname]);

    // When the component mounts, restore the saved scroll position
    useEffect(() => {
        // Check if navigating using browser back/forward buttons
        const isHistoryNavigation = window.history.state?.navigationSource === 'POP';

        if (isHistoryNavigation && !isRestored && SCROLL_POSITIONS[pathname] !== undefined) {
            setTimeout(() => {
                window.scrollTo(0, SCROLL_POSITIONS[pathname]);
                setIsRestored(true);
            }, 100);
        } else if (!isHistoryNavigation) {
            // If not using back/forward, reset to top
            window.scrollTo(0, 0);
        }
    }, [pathname, isRestored]);

    return <>{children}</>;
};

export default ScrollMemory;
